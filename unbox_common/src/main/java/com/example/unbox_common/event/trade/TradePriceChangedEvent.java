package com.example.unbox_common.event.trade;

import java.math.BigDecimal;
import java.util.UUID;

// ✅ Record 사용 (추천)
// 1. 자동으로 모든 필드가 private final이 됨 (불변성 보장)
// 2. 생성자, Getter, toString, equals, hashCode 자동 생성
// 3. Lombok 어노테이션 불필요
public record TradePriceChangedEvent( UUID productId, UUID optionId ,BigDecimal newLowestPrice) {
}