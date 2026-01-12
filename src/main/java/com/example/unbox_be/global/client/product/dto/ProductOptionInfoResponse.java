package com.example.unbox_be.global.client.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionInfoResponse {
    private UUID productOptionId;
    private UUID productId;
    private String productName;
    private String modelNumber;
    private String optionName;
    private String imageUrl;
    private UUID brandId;
    private String brandName;
}
