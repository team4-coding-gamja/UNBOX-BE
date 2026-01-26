package com.example.unbox_payment.payment.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentForSettlementResponse {
    private UUID paymentId;
    private UUID orderId;
    private Long sellerId;
    private BigDecimal amount;
    private String status;
    private String paymentKey;
}
