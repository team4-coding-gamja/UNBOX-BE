package com.example.unbox_trade.common.client.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionForOrderInfoResponse {

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID productOptionId;
    private String productOptionName;

    private Long brandId;
    private String brandName;
}
