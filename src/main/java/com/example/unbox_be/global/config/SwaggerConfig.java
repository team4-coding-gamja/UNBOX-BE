package com.example.unbox_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
public class SwaggerConfig {

    // =========================================================
    // ✅ Group 분리
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
                .pathsToExclude("/api/admin/**", "/api/test/**", "/api/products/requests/**", "/api/reviews/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi sellerApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("3. 판매자 서비스")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**", "/api/test/**", "/api/orders/**", "/api/payment/**", "/api/reviews/**", "/api/products/requests/**", "/api/products/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("4. 관리자 서비스")
                .pathsToMatch("/api/admin/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    // =========================================================
    // ✅ OpenAPI 기본 정보 + JWT
    // =========================================================
    @Bean
    public OpenAPI openAPI() {
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
     * ✅ 목표
     * - 태그 이름 표준화 (tagMap 기준)
     * - 태그 내부 operation 정렬: POST → GET → PATCH/PUT → DELETE
     * - 화면(summary)에 숫자(prefix) 표시 ❌
     *
     * ✅ 방법
     * - Swagger UI 정렬 키로 실제로 쓰이는 operationId를 조작
     * - operationId 앞에 A_/B_/C_/D_ prefix를 붙여 알파벳 정렬로 강제
     * =========================================================
     */
    @Bean
    public OpenApiCustomizer swaggerSortCustomizer() {

        final Map<String, String> tagMap = new LinkedHashMap<>();

        // ===== TEST =====
        tagMap.put("⚠️ 테스트 / 부트스트랩", "[테스트] 마스터 계정 생성");

        // ===== USER =====
        tagMap.put("사용자 인증", "[사용자] 인증 관리");
        tagMap.put("상품 등록 요청 관리", "[사용자] 상품 등록 요청 관리");
        tagMap.put("상품 관리", "[사용자] 상품 관리");
        tagMap.put("판매입찰 관리", "[사용자] 판매입찰 관리");
        tagMap.put("주문 관리", "[사용자] 주문 관리");
        tagMap.put("결제 관리", "[사용자] 결제 관리");
        tagMap.put("리뷰 관리", "[사용자] 리뷰 관리");

        tagMap.put("회원 관리", "[사용자] 회원 관리");
        tagMap.put("위시리스트 관리", "[사용자] 위시리스트(찜) 관리");
        tagMap.put("장바구니 관리", "[사용자] 장바구니 관리");

        // ===== ADMIN =====
        tagMap.put("[관리자] 인증 관리", "[관리자] 인증 관리");
        tagMap.put("[관리자] 상품 등록 요청 관리", "[관리자] 상품 등록 요청 관리");
        tagMap.put("[관리자] 브랜드 관리", "[관리자] 브랜드 관리");
        tagMap.put("[관리자] 상품 관리", "[관리자] 상품 관리");
        tagMap.put("[관리자] 상품 옵션 관리", "[관리자] 상품 옵션 관리");
        tagMap.put("[관리자] 주문 관리", "[관리자] 주문 관리");

        tagMap.put("[관리자] 판매입찰 관리", "[관리자] 판매입찰 관리");
        tagMap.put("[관리자] 스태프 관리", "[관리자] 스태프 관리");
        tagMap.put("[관리자] 사용자 관리", "[관리자] 사용자 관리");


        return openApi -> {

            // ✅ 이 문서에서 실제로 사용된(치환된) 태그만 모으기
            LinkedHashSet<String> usedStandardTags = new LinkedHashSet<>();

            Paths paths = openApi.getPaths();
            if (paths != null) {
                for (Map.Entry<String, PathItem> e : paths.entrySet()) {

                    String path = e.getKey();
                    PathItem item = e.getValue();
                    if (item == null) continue;

                    Map<PathItem.HttpMethod, Operation> ops = item.readOperationsMap();
                    if (ops == null) continue;

                    for (Map.Entry<PathItem.HttpMethod, Operation> opEntry : ops.entrySet()) {

                        PathItem.HttpMethod method = opEntry.getKey();
                        Operation op = opEntry.getValue();
                        if (op == null) continue;

                        // -------------------------------------------------
                        // 1) 태그 치환 + 실제 사용 태그 수집
                        // -------------------------------------------------
                        if (op.getTags() != null && !op.getTags().isEmpty()) {
                            List<String> replaced = op.getTags().stream()
                                    .map(t -> tagMap.getOrDefault(t, t))
                                    .distinct()
                                    .toList();

                            op.setTags(replaced);
                            usedStandardTags.addAll(replaced);
                        }

                        // -------------------------------------------------
                        // 2) 숫자 없이 정렬 강제: operationId에만 prefix
                        //    POST(A_) → GET(B_) → PATCH/PUT(C_) → DELETE(D_)
                        // -------------------------------------------------
                        String methodPrefix = methodOrderPrefix(method);

                        // operationId가 없으면 스프링독이 자동 생성하는 경우가 있는데,
                        // 커스터마이징 단계에서 null일 수 있어 안전 처리
                        if (op.getOperationId() == null || op.getOperationId().isBlank()) {
                            // path + method 기반으로 유니크하게 생성 (충돌 방지)
                            String generated = (method != null ? method.name() : "UNKNOWN")
                                    + "_" + safePathToId(path);
                            op.setOperationId(methodPrefix + generated);
                        } else {
                            String oid = op.getOperationId();
                            if (!oid.startsWith(methodPrefix)) {
                                op.setOperationId(methodPrefix + oid);
                            }
                        }

                        // -------------------------------------------------
                        // ✅ summary는 건드리지 않는다 (숫자 표시 X)
                        // -------------------------------------------------
                    }
                }
            }

            // ✅ “이 문서에서 실제 사용된 태그만” + tagMap 순서대로 tags 세팅
            openApi.setTags(buildStandardTags(tagMap, usedStandardTags));
        };
    }

    /**
     * Swagger UI에서 operationsSorter=alpha일 때,
     * operationId를 알파벳으로 정렬하므로 prefix로 순서를 강제한다.
     */
    private String methodOrderPrefix(PathItem.HttpMethod method) {
        if (method == null) return "Z_";
        return switch (method) {
            case POST -> "A_";
            case GET -> "B_";
            case PATCH, PUT -> "C_";
            case DELETE -> "D_";
            default -> "Z_";
        };
    }

    /**
     * path를 operationId에 넣기 안전한 문자열로 변환
     * 예) /api/admin/staff/{adminId} -> api_admin_staff_adminId
     */
    private String safePathToId(String path) {
        if (path == null) return "unknown_path";
        return path
                .replaceAll("^/+", "")
                .replace("/", "_")
                .replace("{", "")
                .replace("}", "")
                .replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * tagMap.values() 순서를 유지하되,
     * 이 문서에서 실제로 사용된 태그만 남김
     */
    private List<Tag> buildStandardTags(Map<String, String> tagMap, Set<String> usedStandardTags) {
        LinkedHashSet<String> orderedAll = new LinkedHashSet<>(tagMap.values());

        List<Tag> tags = new ArrayList<>();
        for (String name : orderedAll) {
            if (usedStandardTags.contains(name)) {
                tags.add(new Tag().name(name));
            }
        }
        return tags;
    }
}