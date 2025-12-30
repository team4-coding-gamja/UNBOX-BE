package com.example.unbox_be.global.security.jwt;

import com.example.unbox_be.global.error.exception.CustomAuthenticationException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter { // 한 요청당 한 번만 실행되는 필터 클래스

    private final JwtUtil jwtUtil; // JWT 관련 유틸리티 클래스

    public JwtFilter(JwtUtil jwtUtil) { // 생성자에서 JWTUtil 객체를 주입받음
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/signup")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/reissue")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 헤더에서 Authorization 값을 가져옴
        String authorization = request.getHeader("Authorization");

        // ✅ 토큰이 없으면 인증 안 된 상태로 그냥 넘김
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " 부분을 제거하고 순수한 토큰 값만 추출
        String token = authorization.split(" ")[1];

        // 토큰의 만료 여부 확인
        if (jwtUtil.isExpired(token)) {
            throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED);
        }
        if (jwtUtil.isBlacklisted(token)) {
            throw new CustomAuthenticationException(ErrorCode.TOKEN_LOGOUT);
        }

        // 토큰에서 username과 role을 추출
        String email = jwtUtil.getUsername(token);
        String role = jwtUtil.getRole(token);

        // CustomUserDetails 직접 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(email, "", role);

        // 스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        // 시큐리티 컨텍스트에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
