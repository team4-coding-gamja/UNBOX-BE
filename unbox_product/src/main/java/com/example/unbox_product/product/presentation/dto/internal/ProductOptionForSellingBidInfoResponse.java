package com.example.unbox_product.product.presentation.dto.internal;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProductOptionForSellingBidInfoResponse {

    private UUID productOptionId;
    private String productOptionName; // Size or option name
    private UUID productId; // Product ID
    private String productName; // Product name
    private String modelNumber;
    private String productImageUrl; // Product image URL
    private UUID brandId;
    private String brandName;
}
