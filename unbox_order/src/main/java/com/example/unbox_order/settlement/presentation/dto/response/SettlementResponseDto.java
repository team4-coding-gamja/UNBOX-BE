package com.example.unbox_order.settlement.presentation.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SettlementResponseDto {
    private UUID settlementId;
    private UUID orderId;
    private UUID paymentId;
    private Long sellerId;
    private BigDecimal totalAmount;
    private BigDecimal settlementAmount;
    private BigDecimal feesAmount;
    private String settlementStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
