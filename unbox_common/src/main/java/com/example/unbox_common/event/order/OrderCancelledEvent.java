package com.example.unbox_common.event.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
                UUID orderId,
                UUID sellingBidId,
                UUID buyingBidId,
                Long buyerId,
                Long sellerId,
                String reason,
                LocalDateTime cancelledAt) {

        public static OrderCancelledEvent of(
                        UUID orderId,
                        UUID sellingBidId,
                        UUID buyingBidId,
                        Long buyerId,
                        Long sellerId,
                        String reason) {
                return new OrderCancelledEvent(
                                orderId,
                                sellingBidId,
                                buyingBidId,
                                buyerId,
                                sellerId,
                                reason,
                                LocalDateTime.now());
        }
}
