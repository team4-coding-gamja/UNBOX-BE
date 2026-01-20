package com.example.unbox_trade.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    servers = {
        @Server(url = "/", description = "Default Server URL")
    }
)
public class SwaggerConfig {

    @Bean
    public io.swagger.v3.oas.models.OpenAPI tradeOpenAPI() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearer-key", new io.swagger.v3.oas.models.security.SecurityScheme()
                                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-key"))
                .info(new Info()
                        .title("Unbox Trade Service API")
                        .description("MSA Trade Service API Specs")
                        .version("1.0.0"));
    }
}
