package com.example.unbox_be.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 필요하다면 여기서 쓰레드 풀(Thread Pool) 사이즈 등을 상세 설정할 수 있습니다.
    // 기본 설정으로도 충분히 동작합니다.
}