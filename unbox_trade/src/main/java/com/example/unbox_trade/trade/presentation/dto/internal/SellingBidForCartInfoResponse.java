package com.example.unbox_trade.trade.presentation.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidForCartInfoResponse {
    private UUID sellingId;
    private Long sellerId;
    private UUID productOptionId;
    private BigDecimal price;
    private String sellingStatus;

    private UUID productId;
    private String productName;
    private String productOptionName;
    private String productImageUrl;
    private String modelNumber;
    private UUID brandId;
    private String brandName;
}
