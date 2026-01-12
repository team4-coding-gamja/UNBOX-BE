package com.example.unbox_be.global.client.order.dto;

import com.example.unbox_be.domain.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderForReviewInfoResponse {
    private UUID id;
    private Long buyerId;
    private BigDecimal price;
    private OrderStatus status;
    private UUID productOptionId;
}
