package com.example.unbox_product.common.config;

import com.example.unbox_common.config.SwaggerSortUtil;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@OpenAPIDefinition(
    servers = {
        @Server(url = "/", description = "Default Server URL")
    }
)
public class SwaggerConfig {

    // unbox_common 의 설정을 그대로 쓰지 않고, 여기서 구체적인 API 명세를 정의.
    // CommonSwaggerConfig를 상속받거나 새로 빈을 정의해도 됨.
    // 여기서는 CommonSwaggerConfig가 빈으로 등록되므로(scanBasePackages), 추가적인 Customizer만 등록하면 됨.
    
    // 만약 Info를 덮어쓰고 싶다면 여기서 OpenAPI 빈을 새로 정의.
    // 하지만 CommonSwaggerConfig의 OpenAPI 빈이 있으면 충돌날 수 있음.
    // 확인 결과 CommonSwaggerConfig는 @Bean OpenAPI openAPI()를 가지고 있음.
    // Spring Boot는 이름이 같은 빈이 있으면 오버라이딩하거나 충돌남.
    // unbox_product에 맞는 Title로 바꾸기 위해 여기서 OpenAPI 빈을 재정의하는 것이 좋음.

    @Bean
    public io.swagger.v3.oas.models.OpenAPI productOpenAPI() {
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
                        .title("Unbox Product Service API")
                        .description("MSA Product Service API Specs")
                        .version("1.0.0"));
    }

    @Bean
    public OpenApiCustomizer swaggerSortCustomizer() {
        final Map<String, String> tagMap = new LinkedHashMap<>();
        tagMap.put("AI", "AI");
        tagMap.put("[관리자] 상품 관리", "[관리자] 상품 관리");
        tagMap.put("[관리자] 상품 옵션 관리", "[관리자] 상품 옵션 관리");
        tagMap.put("상품 관리", "상품 관리");
        tagMap.put("[관리자] 브랜드 관리", "[관리자] 브랜드 관리");
        tagMap.put("리뷰 관리", "리뷰 관리");

        return SwaggerSortUtil.createSorter(tagMap);
    }
}
