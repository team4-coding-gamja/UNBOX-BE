package com.example.unbox_payment.common.security;

import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ✅ 부하 테스트를 위한 인증 필터
 * - X-Test-User-ID 헤더가 있으면 해당 ID로 인증 객체를 강제 주입함.
 */
@Slf4j
public class LoadTestAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String testUserId = request.getHeader("X-Test-User-ID");

        if (testUserId != null && !testUserId.isBlank()) {
            try {
                Long userId = Long.parseLong(testUserId);
                log.debug("[LoadTestAuth] Matching X-Test-User-ID: {} for URI: {}", userId, request.getRequestURI());

                CustomUserDetails userDetails = CustomUserDetails.ofUserIdOnly(userId,
                        "test-" + userId + "@example.com", "ROLE_USER");
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                log.warn("[LoadTestAuth] Invalid X-Test-User-ID header: {}", testUserId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
