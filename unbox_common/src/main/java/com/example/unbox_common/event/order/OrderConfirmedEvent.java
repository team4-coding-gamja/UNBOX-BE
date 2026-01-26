package com.example.unbox_common.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        Long userId, // 구매자 ID (누가 확정했는지)
        LocalDateTime confirmedAt
) {
    public static OrderConfirmedEvent of(UUID orderId, Long userId) {
        return new OrderConfirmedEvent(orderId, userId, LocalDateTime.now());
    }
}
