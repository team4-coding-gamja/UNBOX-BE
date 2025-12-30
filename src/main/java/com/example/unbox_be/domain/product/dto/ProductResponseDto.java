package com.example.unbox_be.domain.product.dto;

import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    private UUID id;
    private BrandDto brand;
    private String name;
    private String modelNumber;
    private Category category;
    private String imageUrl;
    private List<String> options;

    @Getter
    @Builder
    public static class BrandDto {
        private UUID id;
        private String name;
    }

    public static ProductResponseDto from(Product product, List<ProductOption> options) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .brand(BrandDto.builder()
                        .id(product.getBrand().getId())
                        .name(product.getBrand().getName())
                        .build())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .options(options.stream()
                        .map(ProductOption::getOption)
                        .collect(Collectors.toList()))
                .build();
    }
}
