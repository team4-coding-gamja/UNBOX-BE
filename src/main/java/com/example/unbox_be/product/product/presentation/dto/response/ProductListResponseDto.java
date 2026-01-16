package com.example.unbox_be.product.product.presentation.dto.response;

import com.example.unbox_be.product.product.domain.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListResponseDto {

    private UUID productId;
    private String productName;
    private String modelNumber;
    private Category category;
    private String productImageUrl;

    private UUID brandId;
    private String brandName;

    private Integer lowestPrice;
}

