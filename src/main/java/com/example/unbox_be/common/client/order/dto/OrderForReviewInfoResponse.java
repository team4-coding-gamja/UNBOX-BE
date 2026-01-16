package com.example.unbox_be.common.client.order.dto;

import com.example.unbox_be.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OrderForReviewInfoResponse {

    private Long buyerId;
    private String buyerNickname;

    private OrderStatus orderStatus;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID productOptionId;
    private String productOptionName;

    private String brandName;
}
