package com.example.unbox_payment;

// CI/CD Deploy: 2026-01-27 - Kafka MSK Integration Complete - Full Stack Test
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {"com.example.unbox_payment", "com.example.unbox_common"})
public class UnboxPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnboxPaymentApplication.class, args);
    }

}
