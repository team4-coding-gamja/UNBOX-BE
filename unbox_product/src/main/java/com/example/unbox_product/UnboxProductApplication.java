package com.example.unbox_product;

// CI/CD Deploy: 2026-01-27 - Kafka MSK Integration Complete - Full Stack Test
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"com.example.unbox_product", "com.example.unbox_common"})
@EnableFeignClients
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class UnboxProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnboxProductApplication.class, args);
    }
}
