package com.example.unbox_trade.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 1. 현재 들어온 HTTP 요청을 가져옴
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 2. 헤더에서 Authorization (Bearer 토큰)을 꺼냄
                String token = request.getHeader("Authorization");

                // 3. Feign 요청 헤더에 그대로 집어넣음
                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}