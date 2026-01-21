package com.example.unbox_user.order.order.event;

import com.example.unbox_user.order.order.entity.Order;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OrderCreatedEvent(
        UUID orderId,
        UUID sellingBidId,
        Long buyerId,
        Long sellerId,
        LocalDateTime occurredAt) {
    // 생성 시점에 시간을 자동 할당하는 정적 팩토리 메서드나 커스텀 생성자
    public static OrderCreatedEvent from(Order order) {
        return OrderCreatedEvent.builder()
                .orderId(order.getId())
                .sellingBidId(order.getSellingBidId())
                .buyerId(order.getBuyer().getId())
                .sellerId(order.getSeller().getId())
                .occurredAt(LocalDateTime.now())
                .build();
    }
}