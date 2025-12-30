package com.example.unbox_be.global.security.login;

import com.example.unbox_be.domain.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_be.domain.auth.dto.response.UserTokenResponseDto;
import com.example.unbox_be.global.error.exception.CustomAuthenticationException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import com.example.unbox_be.global.security.jwt.JwtUtil;
import com.example.unbox_be.global.security.token.RefreshTokenRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public LoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, RefreshTokenRedisRepository refreshTokenRedisRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            UserLoginRequestDto requestDto =
                    objectMapper.readValue(request.getInputStream(), UserLoginRequestDto.class);

            log.info("[LoginFilter] 인증 시도 - Email: {}", requestDto.getEmail());

            if (requestDto.getEmail() == null || requestDto.getEmail().isBlank()
                    || requestDto.getPassword() == null || requestDto.getPassword().isBlank()) {
                throw new CustomAuthenticationException(ErrorCode.USERNAME_OR_PASSWORD_MISSING);
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword());

            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 본문을 읽는 중 오류 발생", e);
        }
    }

    // ✅ 로그인 성공 시: 토큰 발급 + Redis 저장 + 응답 내려주기
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            jakarta.servlet.FilterChain chain, Authentication authentication)
            throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        String access = jwtUtil.createAccessToken(email, role, 60 * 60 * 1000L);        // 1시간
        String refresh = jwtUtil.createRefreshToken(email, 60L * 60 * 60 * 1000L);     // 60시간(예시)

        // Redis에 Refresh 저장
        refreshTokenRedisRepository.saveRefreshToken(email, refresh);

        // 응답 설정
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.addHeader("Authorization", "Bearer " + access);
        response.addCookie(createRefreshCookie(refresh));

        // 바디(JSON)
        UserTokenResponseDto dto = UserTokenResponseDto.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();

        objectMapper.writeValue(response.getWriter(), dto);

        log.info("[LoginFilter] 로그인 성공 - 토큰 발급 완료: {}", email);
    }

    // ✅ 로그인 실패 시: 401 JSON 내려주기
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        log.warn("[LoginFilter] 로그인 실패: {}", failed.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(),
                new com.example.unbox_be.global.error.ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        "아이디 또는 비밀번호가 일치하지 않습니다."
                )
        );
    }

    private Cookie createRefreshCookie(String refreshToken) {
        Cookie cookie = new Cookie("refresh", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (60L * 60 * 60)); // 60시간(초)
        // HTTPS면 true
        // cookie.setSecure(true);
        // 쿠키 기반 refresh면 SameSite 설정도 실무에서 중요(자바 서블릿 Cookie만으로는 한계 → ResponseHeader로 세팅하는 방식 많이 씀)
        return cookie;
    }
}
