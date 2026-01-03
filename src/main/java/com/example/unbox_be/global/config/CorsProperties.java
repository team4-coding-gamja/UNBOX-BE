package com.example.unbox_be.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// yml의 "cors" 로 시작하는 설정을 이 객체로 매핑합니다.
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins
) {}