package com.example.unbox_common.event.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderShipmentExpiredEvent(
        UUID orderId,
        UUID sellingBidId,
        UUID buyingBidId,
        UUID paymentId,
        Long buyerId,
        Long sellerId,
        BigDecimal price
) {
    public static OrderShipmentExpiredEvent of(UUID orderId, UUID sellingBidId, UUID buyingBidId,
                                               UUID paymentId, Long buyerId, Long sellerId, BigDecimal price) {
        return new OrderShipmentExpiredEvent(orderId, sellingBidId, buyingBidId, paymentId, buyerId, sellerId, price);
    }
}