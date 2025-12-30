package com.example.unbox_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // SecurityScheme 객체를 생성하여 JWT 인증 방식을 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)  // 인증 타입을 HTTP로 설정
                .scheme("bearer")  // 인증 방식으로 Bearer 사용
                .bearerFormat("JWT")  // Bearer 토큰의 형식을 JWT로 설정
                .in(SecurityScheme.In.HEADER)  // 토큰을 HTTP 헤더에 포함하도록 설정
                .name("Authorization");  // Authorization 헤더에 토큰을 담기 위한 이름 설정

        // SecurityRequirement 객체를 생성하여 API에 보안 요구 사항 추가
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearer-key");

        // OpenAPI 객체를 반환하면서 보안 설정과 API 정보를 설정
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key", securityScheme))  // 보안 스키마를 컴포넌트에 추가
                .addSecurityItem(securityRequirement)  // 보안 요구 사항을 OpenAPI에 추가
                .info(apiInfo());  // API 정보 추가
    }

    private Info apiInfo() {
        return new Info()
                .title("Unbox 프로젝트 API")
                .description("Unbox 백엔드 API 명세서입니다.")
                .version("1.0.0");
    }
}
