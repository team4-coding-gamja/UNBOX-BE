package com.example.unbox_common.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderExpiredEvent(
        UUID orderId,
        UUID sellingBidId,
        UUID buyingBidId,
        LocalDateTime expiredAt) {
    public static OrderExpiredEvent ofSelling(UUID orderId, UUID sellingBidId) {
        return new OrderExpiredEvent(orderId, sellingBidId, null, LocalDateTime.now());
    }

    public static OrderExpiredEvent ofBuying(UUID orderId, UUID buyingBidId) {
        return new OrderExpiredEvent(orderId, null, buyingBidId, LocalDateTime.now());
    }
}
