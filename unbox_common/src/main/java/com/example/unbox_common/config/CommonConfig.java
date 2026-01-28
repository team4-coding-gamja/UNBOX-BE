package com.example.unbox_common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CorsProperties.class})
public class CommonConfig {
    // 나중에 공통으로 필요한 Bean 설정이 있다면 여기에 추가
}