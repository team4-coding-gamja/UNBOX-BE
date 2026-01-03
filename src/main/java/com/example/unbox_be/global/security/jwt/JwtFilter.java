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

        if (request.getMethod().equals("OPTIONS")) {
            log.info("[JwtFilter] OPTIONS 요청(Preflight) → 통과");
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");

        // 토큰이 없거나 Bearer 형식이 아니면 -> "로그인 안 했네? 일단 지나가" (비인증 요청)
        // 이후 SecurityConfig에서 permitAll이면 통과, 아니면 403 에러가 남.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.info("[JwtFilter] Authorization 헤더 없음 또는 Bearer 아님 → 비인증 요청으로 통과: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.split(" ")[1];

            // 토큰 만료 검사
            if (jwtUtil.isExpired(token)) {
                log.warn("[JwtFilter] JWT 만료됨");
                throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED);
            }

            // 로그아웃된 토큰 검사
            if (jwtUtil.isBlacklisted(token)) {
                log.warn("[JwtFilter] JWT 블랙리스트 토큰");
                throw new CustomAuthenticationException(ErrorCode.TOKEN_LOGOUT);
            }

            // 토큰 정보 추출 및 SecurityContext 저장
            String email = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);
            Long id = jwtUtil.getUserId(token);

            CustomUserDetails customUserDetails;
            if ("ROLE_USER".equals(role)) {
                customUserDetails = CustomUserDetails.ofUserIdOnly(id, email, role);
            } else {
                customUserDetails = CustomUserDetails.ofAdminIdOnly(id, email, role);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customUserDetails, null, customUserDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("[JwtFilter] SecurityContext 인증 정보 저장 완료 - User: {}, Role: {}", email, role);

        } catch (CustomAuthenticationException e) {
            // 필터 내부에서 발생한 인증 예외는 여기서 잡히지 않고
            // 보통 AuthenticationEntryPoint로 넘어갑니다.
            // 하지만 명시적으로 로그를 남기기 위해 catch를 둘 수 있습니다.
            request.setAttribute("exception", e); // EntryPoint에서 처리하도록 속성 저장
            throw e;
        } catch (Exception e) {
            log.error("[JwtFilter] 토큰 검증 중 알 수 없는 에러", e);
            // 필요하다면 예외를 던지거나 EntryPoint로 넘김
        }

        filterChain.doFilter(request, response);
    }
}
