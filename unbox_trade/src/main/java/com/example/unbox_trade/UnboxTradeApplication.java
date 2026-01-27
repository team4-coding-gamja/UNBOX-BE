package com.example.unbox_trade;

// CI/CD Deploy: 2026-01-27 - Kafka MSK Integration Complete - Full Stack Test
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.example.unbox_trade", "com.example.unbox_common"})
@EnableFeignClients
@ConfigurationPropertiesScan
@EnableCaching
public class UnboxTradeApplication {

    public static void main(String[] args) {
        // Trade Service - Dev Environment
        SpringApplication.run(UnboxTradeApplication.class, args);
    }
}
