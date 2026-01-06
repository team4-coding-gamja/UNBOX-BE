package com.example.unbox_be;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class UnboxBeApplication {

    public static void main(String[] args) {

        SpringApplication.run(UnboxBeApplication.class, args);
        log.info("=== UNBOX 서버가 정상적으로 실행되었습니다! ===");
    }
}
