package com.example.unbox_be.domain.order.dto.response;

import com.example.unbox_be.domain.order.entity.OrderStatus;
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

    // 3. 상품 옵션 정보 (Nested Structure)
    private ProductOptionInfo productOption;

    // 4. 상품 상세 정보 (Nested Structure)
    private ProductInfo product;

    // --- 내부 클래스 (Nested DTO) ---

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionInfo {
        private UUID id;     // 옵션 ID
        private String size; // 옵션명 (예: 270)
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private UUID id;          // 상품 ID
        private String brandName; // 브랜드명
        private String name;      // 상품명
        private String modelNumber;
        private String imageUrl;
    }
}