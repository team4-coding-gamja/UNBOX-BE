package com.example.unbox_common.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderExpiredEvent(
        UUID orderId,
        UUID sellingBidId,
        LocalDateTime expiredAt
) {
    public static OrderExpiredEvent of(UUID orderId, UUID sellingBidId) {
        return new OrderExpiredEvent(orderId, sellingBidId, LocalDateTime.now());
    }
}
