package com.example.unbox_be.global.security.login;

import com.example.unbox_be.domain.auth.dto.response.UserTokenResponseDto;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import com.example.unbox_be.global.security.jwt.JwtUtil;
import com.example.unbox_be.global.security.token.RefreshTokenRedisRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // 1. 토큰 생성
        String access = jwtUtil.createAccessToken(email, role, 60 * 60 * 1000L); // 1시간
        String refresh = jwtUtil.createRefreshToken(email, 60 * 60 * 60 * 1000L); // 60시간

        // 2. Redis에 Refresh 토큰 저장
        refreshTokenRedisRepository.saveRefreshToken(email, refresh);

        // 3. 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.OK.value());
        response.addHeader("Authorization", "Bearer " + access);
        response.addCookie(createCookie("refresh", refresh));

        // 4. JSON 바디 작성
        UserTokenResponseDto responseDto = UserTokenResponseDto.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();

        objectMapper.writeValue(response.getWriter(), responseDto);
        log.info("[LoginSuccessHandler] 로그인 성공 및 토큰 발급 완료: {}", email);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // HTTPS 환경일 경우 활성화
        return cookie;
    }
}