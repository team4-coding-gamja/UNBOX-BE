package com.example.unbox_common.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated // 유효성 검사 활성화
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        @NotEmpty(message = "CORS allowed origins must not be empty")
        List<String> allowedOrigins
) {}