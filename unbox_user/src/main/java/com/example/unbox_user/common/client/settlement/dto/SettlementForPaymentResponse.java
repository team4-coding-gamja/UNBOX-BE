package com.example.unbox_user.common.client.settlement.dto;

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
public class SettlementForPaymentResponse {
    // ID 정보
    private UUID settlementId;
    private UUID orderId;
    private UUID paymentId;
    private Long sellerId;

    // 금액 정보
    private BigDecimal totalAmount;
    private BigDecimal feesAmount;
    private BigDecimal settlementAmount;

    // 상태 정보
    private String settlementStatus;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}