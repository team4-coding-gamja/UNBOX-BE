package com.example.unbox_be.domain.trade.dto.response;

import com.example.unbox_be.domain.trade.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidResponseDto {
    private UUID id;
    private SellingStatus status;
    private Long sellerId;
    private Integer price;
    private LocalDateTime deadline;
    private LocalDateTime createdAt;

    // 연관된 상품 정보
    private ProductInfo product;
    private String size;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID id;
        private String name;
        private String imageUrl;
    }
}