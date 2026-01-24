package com.example.unbox_trade;

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
        SpringApplication.run(UnboxTradeApplication.class, args);
    }
}
