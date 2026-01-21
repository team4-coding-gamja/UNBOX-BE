package com.example.unbox_product.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 필요한 경우 ArgumentResolver (Pageable은 자동) 설정 추가
    // CORS는 SecurityConfig에서 처리함
}
