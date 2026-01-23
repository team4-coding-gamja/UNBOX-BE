package com.example.unbox_user;

// CI Test: 2026-01-23!!!!!!!!!
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.cloud.openfeign.EnableFeignClients;

@Slf4j
@EnableAsync
@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = "com.example")
public class UnboxBeApplication {

    public static void main(String[] args) {

        SpringApplication.run(UnboxBeApplication.class, args);
        log.info("=== UNBOX 서버가 정상적으로 실행되었습니다! ===");
        log.info("=== CD 자동 배포 테스트 중입니다 ===");
    }
}
