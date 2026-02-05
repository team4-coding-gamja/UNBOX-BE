package com.example.unbox_common.event.trade;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BuyingBidMatchedEvent(
        UUID buyingBidId,
        Long buyerId,
        Long sellerId,
        String productName,
        String productOptionName,
        BigDecimal price,
        LocalDateTime matchedAt
) { }
