package com.example.unbox_be.common.security.login;

import com.example.unbox_be.common.error.ErrorResponse;
import com.example.unbox_be.user.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_be.user.auth.dto.response.UserTokenResponseDto;
import com.example.unbox_be.common.error.exception.CustomAuthenticationException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import com.example.unbox_be.common.security.auth.CustomUserDetails;
import com.example.unbox_be.common.security.jwt.JwtConstants;
import com.example.unbox_be.common.security.jwt.JwtUtil;
import com.example.unbox_be.common.security.token.RefreshTokenRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    private final ObjectMapper objectMapper;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;

    public LoginFilter(
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            RefreshTokenRedisRepository refreshTokenRedisRepository,
            ObjectMapper objectMapper
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRedisRepository = refreshTokenRedisRepository;
        this.objectMapper = objectMapper;

        setFilterProcessesUrl("/api/auth/login");
        log.info("[LoginFilter] LoginFilter 생성자 주입");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            UserLoginRequestDto requestDto =
                    objectMapper.readValue(request.getInputStream(), UserLoginRequestDto.class);

            log.info("[LoginFilter/attemptAuthentication] 1. loginDTO로 객체 변환 email:{}", requestDto.getEmail());

            if (requestDto.getEmail() == null || requestDto.getEmail().isBlank()
                    || requestDto.getPassword() == null || requestDto.getPassword().isBlank()) {
                throw new CustomAuthenticationException(ErrorCode.USERNAME_OR_PASSWORD_MISSING);
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword());

            log.info("[LoginFilter/attemptAuthentication] 2. UsernamePasswordAuthenticationToken 생성 authToken: {}", authToken);

            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 본문을 읽는 중 오류 발생", e);
        }
    }

    // 로그인 성공 시: 토큰 발급 + Redis 저장 + 응답 내려주기
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            jakarta.servlet.FilterChain chain, Authentication authentication)
            throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        Long userId = userDetails.getUserId();
        Long adminId = userDetails.getAdminId();
        log.info("[LoginFilter/successfulAuthentication] 3. 인증된 사용자 정보 가져오기: {}", email);

        String role = authentication.getAuthorities().iterator().next().getAuthority();
        log.info("[LoginFilter/successfulAuthentication] 4. 인증된 사용자 권한 가져오기: {}", role);

        String access;
        String refresh;

        if (userId != null) {
            access = jwtUtil.createAccessToken(userId, email, role, JwtConstants.ACCESS_TOKEN_EXPIRE_MS);
            refresh = jwtUtil.createRefreshToken(userId, email, role, JwtConstants.REFRESH_TOKEN_EXPIRE_MS);
        } else if (adminId != null) {
            access = jwtUtil.createAdminAccessToken(adminId, email, role, JwtConstants.ACCESS_TOKEN_EXPIRE_MS);
            refresh = jwtUtil.createAdminRefreshToken(adminId, email, role, JwtConstants.REFRESH_TOKEN_EXPIRE_MS);
        } else {
            throw new CustomAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
        log.info("[LoginFilter/successfulAuthentication] 5. JWT 토큰 생성 - Access: {}, Refresh: {}", access, refresh);

        // Redis에 Refresh 저장
        refreshTokenRedisRepository.saveRefreshToken(email, refresh);
        log.info("[LoginFilter/successfulAuthentication] 6. Redis에 Refresh 토큰 저장 완료 email={}", email);

        // 응답 설정
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Access는 헤더로
        response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);

        // Refresh는 쿠키로 (HttpOnly)
        // SameSite까지 하려면 Set-Cookie 헤더 직접 세팅 권장
        addRefreshCookie(response, refresh);
        log.info("[LoginFilter/successfulAuthentication/addRefreshCookie] 7. Refresh 토큰을 쿠키로 추가");

        // 바디(JSON)
        UserTokenResponseDto dto = UserTokenResponseDto.builder()
                .accessToken(access)
                .role(role)
                .build();
        log.info("[LoginFilter/successfulAuthentication] 8. JWT TokenDTO 생성 완료: {}", dto);

        objectMapper.writeValue(response.getWriter(), dto);
        log.info("[LoginFilter/successfulAuthentication] 9. JWT 토큰 HTTP 헤더 및 쿠키 설정 완료");
    }

    // 로그인 실패 시: 401 JSON 내려주기
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        log.warn("[LoginFilter] 로그인 실패: {}", failed.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        "아이디 또는 비밀번호가 일치하지 않습니다."
                )
        );
    }

    // refresh 쿠키 추가
    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        boolean secure = false; // ✅ 배포(HTTPS)에서는 true로 바꾸기
        String sameSite = JwtConstants.DEFAULT_SAMESITE; // 크로스사이트 사용(프론트 분리)이라면 "None" + secure=true 필요

        // Set-Cookie 헤더 직접 추가 (SameSite 포함)
        String cookieValue = String.format(
                "refresh=%s; Max-Age=%d; Path=/; HttpOnly%s; SameSite=%s",
                refreshToken,
                JwtConstants.REFRESH_COOKIE_MAX_AGE_SEC,
                secure ? "; Secure" : "",
                sameSite
        );

        response.addHeader(HttpHeaders.SET_COOKIE, cookieValue);
    }
}
