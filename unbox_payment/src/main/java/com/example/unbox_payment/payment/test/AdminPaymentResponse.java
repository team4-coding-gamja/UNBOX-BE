package com.example.unbox_payment.payment.test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminPaymentResponse(
        UUID paymentId,
        UUID orderId,
        Long buyerId,
        Long sellerId,
        BigDecimal amount,
        String method,
        String status,
        LocalDateTime createdAt,
        LocalDateTime approvedAt) {
}
