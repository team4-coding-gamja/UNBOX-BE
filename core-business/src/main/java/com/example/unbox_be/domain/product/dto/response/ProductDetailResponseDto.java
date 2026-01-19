package com.example.unbox_be.domain.product.dto.response;

import com.example.unbox_be.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponseDto {

    private UUID id;
    private String name;
    private String modelNumber;
    private Category category;
    private String imageUrl;

    private UUID brandId;
    private String brandName;

    private Integer lowestPrice;

    private Integer reviewCount;
    private Double averageRating;
}
