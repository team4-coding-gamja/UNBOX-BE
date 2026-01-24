package com.example.unbox_common.event.order;

import java.util.UUID;

public record OrderCancelledEvent(
    UUID orderId,
    UUID sellingBidId,
    Long buyerId,
    Long sellerId,
    String reason
) {
}
