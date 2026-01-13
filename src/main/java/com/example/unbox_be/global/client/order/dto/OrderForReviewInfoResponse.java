package com.example.unbox_be.global.client.order.dto;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderForReviewInfoResponse {

    private UUID id;
    private Long buyerId;
    private Long sellerId;
    private UUID productOptionId;
    private BigDecimal price;
    private OrderStatus status;
    private LocalDateTime completedAt;

    public static OrderForReviewInfoResponse from(Order order) {
        return OrderForReviewInfoResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .sellerId(order.getSellerId())
                .productOptionId(order.getProductOptionId())
                .price(order.getPrice())
                .status(order.getStatus())
                .completedAt(order.getCompletedAt())
                .build();
    }
}
