package com.example.unbox_order.common.client.trade.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BuyingBidForOrderResponse {
    private UUID buyingBidId;
    private Long buyerId;
    private Long sellerId;
    private String buyingStatus;
    private UUID productId;
    private UUID productOptionId;
    private BigDecimal price;

    // 상품 스냅샷
    private String productName;
    private String modelNumber;
    private String productOptionName;
    private String productImageUrl;
    private String brandName;

    @Builder
    public BuyingBidForOrderResponse(UUID buyingBidId, Long buyerId, Long sellerId, String buyingStatus, UUID productId, UUID productOptionId,
            BigDecimal price, String productName, String modelNumber, String productOptionName,
            String productImageUrl, String brandName) {
        this.buyingBidId = buyingBidId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyingStatus = buyingStatus;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.price = price;
        this.productName = productName;
        this.modelNumber = modelNumber;
        this.productOptionName = productOptionName;
        this.productImageUrl = productImageUrl;
        this.brandName = brandName;
    }
}
