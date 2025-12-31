package com.example.unbox_be.global.security.jwt;

import com.example.unbox_be.global.error.exception.CustomAuthenticationException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("[JwtFilter] 요청 진입 - URI: {}", path);

        // 인증 제외 경로
        if (path.startsWith("/api/auth/signup")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/reissue")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")) {

            log.info("[JwtFilter] 인증 제외 경로 → 필터 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 조회
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.info("[JwtFilter] Authorization 헤더 없음 또는 Bearer 아님 → 비인증 요청으로 통과");
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 추출
        String token = authorization.split(" ")[1];
        log.info("[JwtFilter] JWT 추출 완료 - token prefix: {}", token.substring(0, 10));

        // 토큰 만료 검사
        if (jwtUtil.isExpired(token)) {
            log.warn("[JwtFilter] JWT 만료됨");
            throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED);
        }

        // 로그아웃(블랙리스트) 검사
        if (jwtUtil.isBlacklisted(token)) {
            log.warn("[JwtFilter] JWT 블랙리스트 토큰(로그아웃 처리됨)");
            throw new CustomAuthenticationException(ErrorCode.TOKEN_LOGOUT);
        }

        // 토큰 정보 추출
        String email = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        log.info("[JwtFilter] JWT 검증 성공 - email: {}, role: {}", email, role);

        // CustomUserDetails 생성 (JWT 기반)
        CustomUserDetails customUserDetails = new CustomUserDetails(email, "", role);

        log.info("[JwtFilter] CustomUserDetails 생성 완료");

        // Authentication 객체 생성
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        customUserDetails,
                        null,
                        customUserDetails.getAuthorities()
                );

        // SecurityContext 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("[JwtFilter] SecurityContext 인증 정보 저장 완료");

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}
