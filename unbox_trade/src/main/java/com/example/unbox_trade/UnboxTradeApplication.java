package com.example.unbox_trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example.unbox_trade", "com.example.unbox_common"})
@org.springframework.cloud.openfeign.EnableFeignClients
@org.springframework.boot.context.properties.ConfigurationPropertiesScan
public class UnboxTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnboxTradeApplication.class, args);
    }
}
