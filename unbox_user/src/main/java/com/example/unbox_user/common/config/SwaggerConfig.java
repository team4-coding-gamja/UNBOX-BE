package com.example.unbox_user.common.config;

// ✅ 공통 모듈의 정렬 유틸 Import
import com.example.unbox_common.config.SwaggerSortUtil;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@OpenAPIDefinition(servers = {
    @Server(url = "/user", description = "Local Server"),
    @Server(url = "http://unbox-dev-alb-2003561066.ap-northeast-2.elb.amazonaws.com/user", description = "Dev Server (User)")
})
public class SwaggerConfig {

    // =========================================================
    // ✅ Group 분리 (기존 로직 그대로 유지)
    // =========================================================
    @Bean
    public GroupedOpenApi totalApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("1. 전체 API")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi buyerApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("2. 구매자 서비스")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**", "/api/test/**", "/api/products/requests/**", "/api/reviews/**", "/api/users/**", "/api/wishlist/**", "/api/carts/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi sellerApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("3. 판매자 서비스")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**", "/api/test/**", "/api/orders/**", "/api/payment/**", "/api/reviews/**", "/api/products/requests/**", "/api/products/**", "/api/users/**", "/api/wishlist/**", "/api/carts/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("4. 관리자 서비스")
                .pathsToMatch("/api/admin/**")
                .pathsToExclude("/api/test/**", "/api/admin/products/**", "/api/admin/reviews/**", "/api/admin/product-requests/**", "/api/admin/brands/**", "/api/admin/users/**", "/api/admin/bids/selling/**", "/api/admin/staff/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    // =========================================================
    // OpenAPI 기본 정보 + JWT (기존 유지)
    // (CommonSwaggerConfig가 있어도, 여기서 구체적인 제목/설명을 덮어씌우는 게 좋습니다)
    // =========================================================
    @Bean
    public OpenAPI userOpenApi() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-key"))
                .info(new Info()
                        .title("Unbox API 명세서")
                        .description(
                                "Unbox 백엔드 API 명세서입니다.\n\n" +
                                        "API는 전체 API, 사용자 API, 관리자 API로 구분되어 있습니다."
                        )
                        .version("1.0.0"));
    }

    /**
     * =========================================================
     * 리팩토링 포인트
     * - 기존: 복잡한 정렬 로직이 여기 다 들어있었음.
     * - 변경: 태그(TagMap)만 정의하고, 정렬은 unbox_common의 유틸에게 위임!
     * =========================================================
     */
    @Bean
    public OpenApiCustomizer swaggerSortCustomizer() {

        final Map<String, String> tagMap = new LinkedHashMap<>();

        // ===== TEST =====
        tagMap.put("⚠️ 테스트 / 부트스트랩", "[테스트] 마스터 계정 생성");

        // ===== USER (그대로 유지) =====
        tagMap.put("사용자 인증", "[사용자] 인증 관리");
        tagMap.put("회원 관리", "[사용자] 회원 관리");
        tagMap.put("위시리스트 관리", "[사용자] 위시리스트(찜) 관리");
        tagMap.put("장바구니 관리", "[사용자] 장바구니 관리");

        // ===== PRODUCT / ORDER / ETC (그대로 유지) =====
        tagMap.put("상품 등록 요청 관리", "[사용자] 상품 등록 요청 관리");
        tagMap.put("상품 관리", "[사용자] 상품 관리");
        tagMap.put("판매입찰 관리", "[사용자] 판매입찰 관리");
        tagMap.put("주문 관리", "[사용자] 주문 관리");
        tagMap.put("결제 관리", "[사용자] 결제 관리");
        tagMap.put("리뷰 관리", "[사용자] 리뷰 관리");

        // ===== ADMIN (그대로 유지) =====
        tagMap.put("[관리자] 인증 관리", "[관리자] 인증 관리");
        tagMap.put("[관리자] 스태프 관리", "[관리자] 스태프 관리");
        tagMap.put("[관리자] 사용자 관리", "[관리자] 사용자 관리");
        tagMap.put("[관리자] 상품 등록 요청 관리", "[관리자] 상품 등록 요청 관리");
        tagMap.put("[관리자] 브랜드 관리", "[관리자] 브랜드 관리");
        tagMap.put("[관리자] 상품 관리", "[관리자] 상품 관리");
        tagMap.put("[관리자] 상품 옵션 관리", "[관리자] 상품 옵션 관리");
        tagMap.put("[관리자] 주문 관리", "[관리자] 주문 관리");
        tagMap.put("[관리자] 판매입찰 관리", "[관리자] 판매입찰 관리");

        // [핵심] 100줄 넘는 정렬 로직을 1줄로 단축! (Util 사용)
        return SwaggerSortUtil.createSorter(tagMap);
    }
}