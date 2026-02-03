package com.example.unbox_common.event.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID paymentId,
        String paymentKey,
        UUID orderId,
        UUID sellingBidId,
        UUID buyingBidId,
        BigDecimal amount,
        LocalDateTime completedAt) {

    // 하위 호환성 유지 (기존 판매 입찰용)
    public static PaymentCompletedEvent ofSelling(UUID paymentId, String paymentKey, UUID orderId, UUID sellingBidId,
                                                  BigDecimal amount) {
        return new PaymentCompletedEvent(paymentId, paymentKey, orderId, sellingBidId, null, amount,
                LocalDateTime.now());
    }

    public static PaymentCompletedEvent ofBuying(UUID paymentId, String paymentKey, UUID orderId, UUID buyingBidId,
                                                 BigDecimal amount) {
        return new PaymentCompletedEvent(paymentId, paymentKey, orderId, null, buyingBidId, amount,
                LocalDateTime.now());
    }
}
