package com.example.unbox_order.common.client.trade.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class SellingBidForOrderResponse {
    private UUID sellingBidId;
    private Long sellerId;
    private UUID productId;
    private UUID productOptionId;
    private BigDecimal price;
    private String status;
    
    // 상품 스냅샷
    private String productName;
    private String modelNumber;
    private String productOptionName;
    private String productImageUrl;
    private String brandName;

    @Builder
    public SellingBidForOrderResponse(UUID sellingBidId, Long sellerId, UUID productId, UUID productOptionId, BigDecimal price, String status, String productName, String modelNumber, String productOptionName, String productImageUrl, String brandName) {
        this.sellingBidId = sellingBidId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.price = price;
        this.status = status;
        this.productName = productName;
        this.modelNumber = modelNumber;
        this.productOptionName = productOptionName;
        this.productImageUrl = productImageUrl;
        this.brandName = brandName;
    }
}
