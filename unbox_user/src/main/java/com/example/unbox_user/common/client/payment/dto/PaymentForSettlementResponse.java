package com.example.unbox_user.common.client.payment.dto;

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
public class PaymentForSettlementResponse {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String status;
}