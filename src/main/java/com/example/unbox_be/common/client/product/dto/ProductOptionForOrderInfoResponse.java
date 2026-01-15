package com.example.unbox_be.common.client.product.dto;

import com.example.unbox_be.product.product.entity.Product;
import com.example.unbox_be.product.product.entity.ProductOption;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForOrderInfoResponse {
    private UUID id;
    private String productOptionName;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID brandId;
    private String brandName;

    public static ProductOptionForOrderInfoResponse from(ProductOption productOption) {
        Product product = productOption.getProduct();
        return ProductOptionForOrderInfoResponse.builder()
                .id(productOption.getId())
                .productOptionName(productOption.getOption())

                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .productImageUrl(product.getProductImageUrl())

                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .build();
    }
}
