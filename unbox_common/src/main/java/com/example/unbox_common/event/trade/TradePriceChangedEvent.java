package com.example.unbox_common.event.trade;

import java.math.BigDecimal;
import java.util.UUID;

public record TradePriceChangedEvent(UUID productId, UUID productOptionId, BigDecimal price) {
}