package com.example.unbox_payment.common.client.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OrderForReviewInfoResponse {

    // ===== 검증용 필드 (스냅샷에 저장하지 않음) =====
    private Long buyerId; // 권한 검증용
    private String orderStatus; // 주문 완료 상태 검증용

    // ===== 스냅샷 저장 필드 =====
    private String buyerNickname;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID productOptionId;
    private String productOptionName;

    private String brandName;
}
