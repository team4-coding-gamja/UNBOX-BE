package com.example.unbox_be.global.client.product.dto;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForOrderInfoResponse {
    private UUID id; // ProductOption ID
    private String optionName;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String imageUrl;

    private UUID brandId;
    private String brandName;

    public static ProductOptionForOrderInfoResponse from(ProductOption productOption) {
        Product product = productOption.getProduct();
        return ProductOptionForOrderInfoResponse.builder()
                .id(productOption.getId())
                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .optionName(productOption.getOption())
                .imageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .build();
    }
}
