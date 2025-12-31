package com.example.unbox_be.domain.trade.dto.response;

import com.example.unbox_be.domain.trade.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidResponseDto {
    private UUID sellingId;
    private SellingStatus status;
    private Integer price;
    private LocalDateTime deadline;

    // 연관된 상품 정보
    private ProductInfo product;
    private String size;

    @Getter
    @Builder
    public static class ProductInfo {
        private UUID id;
        private String name;
        private String imageUrl;
    }
}