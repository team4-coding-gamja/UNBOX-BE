package com.example.unbox_common.event.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        String paymentKey,
        UUID orderId,
        UUID sellingBidId,
        BigDecimal amount,
        LocalDateTime completedAt
) {
    public static PaymentCompletedEvent of(String paymentKey, UUID orderId, UUID sellingBidId, BigDecimal amount) {
        return new PaymentCompletedEvent(paymentKey, orderId, sellingBidId, amount, LocalDateTime.now());
    }
}
