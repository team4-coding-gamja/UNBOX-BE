package com.example.unbox_product.product.presentation.dto.internal;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForOrderInfoResponse {
    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID brandId;
    private String brandName;
}
