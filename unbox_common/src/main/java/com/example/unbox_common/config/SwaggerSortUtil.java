package com.example.unbox_common.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.*;

public class SwaggerSortUtil {

    // 각 서비스에서 이 메서드를 호출해서 Customizer를 얻어갑니다.
    public static OpenApiCustomizer createSorter(Map<String, String> tagMap) {
        return openApi -> {
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

                        // 1. 태그 치환
                        if (op.getTags() != null && !op.getTags().isEmpty()) {
                            List<String> replaced = op.getTags().stream()
                                    .map(t -> tagMap.getOrDefault(t, t)) // 맵에 없으면 원래 이름 사용
                                    .distinct()
                                    .toList();
                            op.setTags(replaced);
                            usedStandardTags.addAll(replaced);
                        }

                        // 2. 정렬 강제 (prefix 추가)
                        String methodPrefix = methodOrderPrefix(method);
                        if (op.getOperationId() == null || op.getOperationId().isBlank()) {
                            String generated = (method != null ? method.name() : "UNKNOWN") + "_" + safePathToId(path);
                            op.setOperationId(methodPrefix + generated);
                        } else {
                            String oid = op.getOperationId();
                            if (!oid.startsWith(methodPrefix)) {
                                op.setOperationId(methodPrefix + oid);
                            }
                        }
                    }
                }
            }
            // 3. 태그 순서 적용
            openApi.setTags(buildStandardTags(tagMap, usedStandardTags));
        };
    }

    private static String methodOrderPrefix(PathItem.HttpMethod method) {
        if (method == null) return "Z_";
        return switch (method) {
            case POST -> "A_";
            case GET -> "B_";
            case PATCH, PUT -> "C_";
            case DELETE -> "D_";
            default -> "Z_";
        };
    }

    private static String safePathToId(String path) {
        if (path == null) return "unknown_path";
        return path.replaceAll("^/+", "").replace("/", "_").replace("{", "").replace("}", "").replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static List<Tag> buildStandardTags(Map<String, String> tagMap, Set<String> usedStandardTags) {
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