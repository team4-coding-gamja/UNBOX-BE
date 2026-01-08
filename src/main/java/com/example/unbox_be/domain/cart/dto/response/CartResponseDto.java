package com.example.unbox_be.domain.cart.dto.response;

import com.example.unbox_be.domain.trade.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private Long cartId;
    private SellingBidInfo sellingBid;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellingBidInfo {
        private UUID id;
        private BigDecimal price;
        private SellingStatus status;
        private String size;
        private ProductInfo product;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID id;
        private String name;
        private String brandName;
        private String imageUrl;
    }
}
