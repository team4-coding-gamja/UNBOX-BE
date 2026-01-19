package com.example.unbox_be.global.security.jwt;

import com.example.unbox_be.global.error.exception.CustomAuthenticationException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
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
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
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
            String token = authorization.substring(7);
            if (token.isBlank()) {
                log.warn("[JwtFilter] Bearer 토큰이 비어있음");
                throw new CustomAuthenticationException(ErrorCode.INVALID_TOKEN);
            }

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

            Long id;
            CustomUserDetails customUserDetails;

            if ("ROLE_USER".equals(role)) {
                id = jwtUtil.getUserId(token);
                customUserDetails = CustomUserDetails.ofUserIdOnly(id, email, role);
            }
            else if ("ROLE_MASTER".equals(role) || "ROLE_MANAGER".equals(role) || "ROLE_INSPECTOR".equals(role)) {
                id = jwtUtil.getAdminId(token);
                customUserDetails = CustomUserDetails.ofAdminIdOnly(id, email, role);
            }
            else {
                // DB에 없는 이상한 Role이 토큰에 들어있는 경우 -> 보안 위협으로 간주하고 차단
                log.warn("[JwtFilter] 알 수 없는 역할(Role) 감지: {}", role);
                throw new CustomAuthenticationException(ErrorCode.INVALID_TOKEN);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customUserDetails, null, customUserDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("[JwtFilter] SecurityContext 인증 완료 - UserId: {}, Role: {}", id, role);

        } catch (CustomAuthenticationException e) {
            // 이미 잡힌 커스텀 예외는 다시 던져서 EntryPoint로 보냄
            request.setAttribute("exception", e);
            throw e;
        } catch (Exception e) {
            // 예기치 못한 에러(파싱 오류 등)가 났을 때, 보안상 인증 실패로 처리해야 함
            log.error("[JwtFilter] 토큰 검증 중 알 수 없는 에러 발생", e);
            request.setAttribute("exception", new CustomAuthenticationException(ErrorCode.INVALID_TOKEN));
            throw new CustomAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }
}
