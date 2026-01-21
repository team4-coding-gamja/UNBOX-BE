package com.example.unbox_common.config;

import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

public class CorsConfigUtil {

    public static CorsConfiguration getCorsConfig(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();

        // 프론트엔드 주소 (yml에서 주입받은 값)
        config.setAllowedOrigins(properties.allowedOrigins());

        // 공통 허용 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 공통 허용 헤더
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        return config;
    }
}