package com.example.unbox_common.event.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        String paymentKey,
        UUID orderId,
        UUID sellingBidId,
        UUID buyingBidId,
        BigDecimal amount,
        String errorCode,
        String errorMessage,
        LocalDateTime failedAt) {
    public static PaymentFailedEvent of(UUID paymentId, String paymentKey, UUID orderId, UUID sellingBidId,
            UUID buyingBidId, BigDecimal amount, String errorCode, String errorMessage) {
        return new PaymentFailedEvent(paymentId, paymentKey, orderId, sellingBidId, buyingBidId, amount, errorCode,
                errorMessage, LocalDateTime.now());
    }
}
