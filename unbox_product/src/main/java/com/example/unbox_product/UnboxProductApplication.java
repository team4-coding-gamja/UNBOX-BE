package com.example.unbox_product;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Cache test - Testing Docker layer cache efficiency
// Modified at: 2026-01-22 to verify ECR cache reuse

@SpringBootApplication(scanBasePackages = {"com.example.unbox_product", "com.example.unbox_common"})
@EnableFeignClients
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class UnboxProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnboxProductApplication.class, args);
    }
}
