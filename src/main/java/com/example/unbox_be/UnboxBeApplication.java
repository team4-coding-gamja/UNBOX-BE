package com.example.unbox_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication

public class UnboxBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnboxBeApplication.class, args);
    }
}
