package com.example.unbox_order.order.presentation.dto.response;

import com.example.unbox_order.order.domain.entity.OrderStatus;
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
public class OrderDetailResponseDto {

    // 1. 주문 기본 정보
    private UUID orderId;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    // 2. 배송 정보 (개인정보 보호 대상)
    private String receiverName;
    private String receiverPhone;   // 마스킹 처리 권장
    private String receiverAddress; // 마스킹 처리 권장
    private String receiverZipCode;
    private String trackingNumber;  // 운송장 번호

    private ProductOptionInfo productOptionInfo;
    private ProductInfo productInfo;


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionInfo {
        private UUID id;     // 옵션 ID
        private String productOptionName; // 옵션명 (예: 270)
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID id;          // 상품 ID
        private String brandName; // 브랜드명
        private String productName;      // 상품명
        private String modelNumber;
        private String productImageUrl;
    }
}