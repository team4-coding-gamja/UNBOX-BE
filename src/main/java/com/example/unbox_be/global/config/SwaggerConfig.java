package com.example.unbox_be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    // ✅ Group 분리는 유지
    @Bean
    public GroupedOpenApi totalApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("1. 전체 API")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi userApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("2. 사용자 서비스")
                .pathsToMatch("/api/**")
                .pathsToExclude("/api/admin/**", "/api/test/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(OpenApiCustomizer swaggerSortCustomizer) {
        return GroupedOpenApi.builder()
                .group("3. 관리자 서비스")
                .pathsToMatch("/api/admin/**")
                .addOpenApiCustomizer(swaggerSortCustomizer)
                .build();
    }

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
                        .title("Unbox 프로젝트 API")
                        .description("Unbox 백엔드 API 명세서입니다. (태그/CRUD 정렬 강제 적용)")
                        .version("1.0.0"));
    }

    /**
     * ✅ 핵심: (1) 태그 이름을 번호 붙인 “표준 태그명”으로 통일
     *        (2) 각 태그 내부의 operationId / summary에 CRUD prefix를 붙여서 정렬 강제
     *
     * Swagger UI에서 tagsSorter/operationsSorter가 alpha여도,
     * prefix 때문에 항상 원하는 순서로 보이게 됩니다.
     */
    @Bean
    public OpenApiCustomizer swaggerSortCustomizer() {

        final Map<String, String> tagMap = new LinkedHashMap<>();

        // ===== USER 시나리오 =====
        tagMap.put("사용자 인증", "[사용자] 인증 관리");
        tagMap.put("회원 관리", "[사용자] 회원 관리");
        tagMap.put("상품", "[사용자] 상품 관리");
        tagMap.put("wishlist-controller", "[사용자] 찜 관리");
        tagMap.put("selling-bid-controller", "[사용자] 판매입찰 관리");
        tagMap.put("주문 관리", "[사용자] 주문 관리");
        tagMap.put("Payment API", "[사용자] 결제 관리");
        tagMap.put("리뷰 관리", "[사용자] 리뷰 관리");
        tagMap.put("상품 요청", "[사용자] 상품 등록 요청 관리");

        // ===== 관리자 =====
        tagMap.put("[관리자] 인증 관리", "[관리자] 인증 관리");
        tagMap.put("[관리자] 스태프 관리", "[관리자] 스태프 관리");
        tagMap.put("[관리자] 사용자 관리", "[관리자] 사용자 관리");
        tagMap.put("[관리자] 브랜드 관리", "[관리자] 브랜드 관리");
        tagMap.put("[관리자] 상품 관리", "[관리자] 상품 관리");
        tagMap.put("[관리자] 상품 옵션 관리", "[관리자] 상품 옵션 관리");
        tagMap.put("[관리자] 주문 관리", "[관리자] 주문 관리");
        tagMap.put("[관리자] 상품 등록 요청 관리", "[관리자] 상품 등록 요청 관리");

        // ===== TEST =====
        tagMap.put("⚠️ 테스트 / 부트스트랩", "99. 테스트 - 부트스트랩");

        final Pattern alreadyPrefixed = Pattern.compile("^\\d{2}[._ -].*");

        return openApi -> {

            // ✅ 이 문서에서 실제로 쓰인(치환된) 태그만 모으기
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

                        // 1) 태그 치환 + usedTags 수집
                        if (op.getTags() != null && !op.getTags().isEmpty()) {
                            List<String> replaced = op.getTags().stream()
                                    .map(t -> tagMap.getOrDefault(t, t))
                                    .distinct()
                                    .toList();
                            op.setTags(replaced);
                            usedStandardTags.addAll(replaced); // ✅ 이 문서에 실제 쓰인 표준 태그만 모음
                        }

                        // 2) CRUD prefix (너 기존 로직 유지)
                        String crudPrefix = crudPrefix(method, path);
                        String prefixNumDot = crudPrefixToNumberDot(crudPrefix);

                        if (op.getOperationId() != null && !op.getOperationId().isBlank()) {
                            String oid = op.getOperationId();
                            if (!oid.startsWith(crudPrefix + "_")) {
                                op.setOperationId(crudPrefix + "_" + oid);
                            }
                        }

                        if (op.getSummary() != null && !op.getSummary().isBlank()) {
                            String s = op.getSummary();
                            if (!alreadyPrefixed.matcher(s).matches()) {
                                op.setSummary(prefixNumDot + " - " + s);
                            }
                        }
                    }
                }
            }

            // ✅ 마지막에: “이 문서에서 사용된 태그만” + tagMap 순서대로 정렬해서 tags 세팅
            openApi.setTags(buildStandardTags(tagMap, usedStandardTags));
        };
    }


    private List<Tag> buildStandardTags(Map<String, String> tagMap) {
        // tagMap의 value(표준 태그명)에서 중복 제거 + 순서 유지
        LinkedHashSet<String> ordered = new LinkedHashSet<>(tagMap.values());

        List<Tag> tags = new ArrayList<>();
        for (String name : ordered) {
            tags.add(new Tag().name(name));
        }
        return tags;
    }

    private String crudPrefix(PathItem.HttpMethod method, String path) {
        // CRUD 우선순위: CREATE(POST) -> LIST(GET without {id}) -> DETAIL(GET with {id})
        //             -> UPDATE(PATCH/PUT) -> DELETE(DELETE)
        if (method == null) return "99_ETC";

        switch (method) {
            case POST:
                return "01_CREATE";
            case GET:
                // path에 { } 있으면 단건, 없으면 목록으로 간주
                return (path != null && path.contains("{")) ? "03_DETAIL" : "02_LIST";
            case PATCH:
            case PUT:
                return "04_UPDATE";
            case DELETE:
                return "05_DELETE";
            default:
                return "99_ETC";
        }
    }

    private String crudPrefixToNumberDot(String crudPrefix) {
        // Swagger UI에 보여줄 summary prefix
        switch (crudPrefix) {
            case "01_CREATE": return "01. 생성";
            case "02_LIST":   return "02. 목록 조회";
            case "03_DETAIL": return "03. 상세 조회";
            case "04_UPDATE": return "04. 수정";
            case "05_DELETE": return "05. 삭제";
            default:          return "99. 기타";
        }
    }

    private List<Tag> buildStandardTags(Map<String, String> tagMap, Set<String> usedStandardTags) {
        // tagMap.values()의 순서(LinkedHashMap 유지)를 그대로 따르되,
        // 이 문서에서 실제로 쓰인 태그만 남김
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
