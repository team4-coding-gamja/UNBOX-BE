package com.example.unbox_common.event.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        String paymentKey,
        UUID orderId,
        UUID sellingBidId,
        BigDecimal amount,
        LocalDateTime completedAt
) {
    public static PaymentCompletedEvent of(UUID paymentId, String paymentKey, UUID orderId, UUID sellingBidId, BigDecimal amount) {
        return new PaymentCompletedEvent(paymentId, paymentKey, orderId, sellingBidId, amount, LocalDateTime.now());
    }
}
