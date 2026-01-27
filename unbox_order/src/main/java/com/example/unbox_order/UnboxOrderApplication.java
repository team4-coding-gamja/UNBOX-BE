package com.example.unbox_order;

// CI/CD Deploy: 2026-01-27 - CI Pipeline Test Run #2
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.example.unbox_order", "com.example.unbox_common"})
public class UnboxOrderApplication {

    public static void main(String[] args) {
        // Order Service - Dev Environment
        SpringApplication.run(UnboxOrderApplication.class, args);
    }

}
