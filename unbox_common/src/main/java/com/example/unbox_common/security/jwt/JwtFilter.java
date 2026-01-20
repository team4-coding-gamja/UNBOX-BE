package com.example.unbox_common.security.jwt;

import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_common.error.exception.CustomAuthenticationException;
import com.example.unbox_common.error.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(7);

            if (jwtUtil.isExpired(token)) {
                throw new CustomAuthenticationException(ErrorCode.TOKEN_EXPIRED);
            }
            if (jwtUtil.isBlacklisted(token)) {
                throw new CustomAuthenticationException(ErrorCode.TOKEN_LOGOUT);
            }

            String email = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);

            CustomUserDetails userDetails;

            // ✅ 앞서 만든 POJO CustomUserDetails 팩토리 사용
            if ("ROLE_USER".equals(role)) {
                Long userId = jwtUtil.getUserId(token);
                userDetails = CustomUserDetails.ofUserIdOnly(userId, email, role);
            } else {
                Long adminId = jwtUtil.getAdminId(token);
                userDetails = CustomUserDetails.ofAdminIdOnly(adminId, email, role);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (CustomAuthenticationException e) {
            request.setAttribute("exception", e);
            throw e;
        } catch (Exception e) {
            log.error("[JwtFilter] 토큰 검증 오류", e);
            request.setAttribute("exception", new CustomAuthenticationException(ErrorCode.INVALID_TOKEN));
            throw new CustomAuthenticationException(ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }
}