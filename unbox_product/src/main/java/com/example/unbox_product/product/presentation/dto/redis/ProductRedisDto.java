package com.example.unbox_product.product.presentation.dto.redis;

import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

// 조회 성능 최적화를 위해 Redis(캐시)에 저장하는 읽기 전용 데이터 모델

// Redis Key: "prod:info:{id}"
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRedisDto {
    private UUID productId;
    private String name;
    private String modelNumber;
    private String imageUrl;

    private UUID brandId;
    private String brandName;
    private Category category;
    private int reviewCount;
    private double totalScore;
    private List<ProductOptionDto> options;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionDto {
        private UUID optionId;
        private String optionName;

        public static ProductOptionDto from(ProductOption option) {
            return new ProductOptionDto(option.getId(), option.getName());
        }
    }

    // ✅ 변환 로직 구현 (Entity -> Redis DTO)
    public static ProductRedisDto from(Product product, List<ProductOption> options) {
        List<ProductOptionDto> optionDtos = options.stream()
                .map(ProductOptionDto::from)
                .toList();

        return new ProductRedisDto(
                product.getId(),
                product.getName(),
                product.getModelNumber(),
                product.getImageUrl(),
                product.getBrand().getId(),
                product.getBrand().getName(), // Brand Fetch Join 필수
                product.getCategory(),
                product.getReviewCount(),
                product.getTotalScore(),
                optionDtos
        );
    }
}