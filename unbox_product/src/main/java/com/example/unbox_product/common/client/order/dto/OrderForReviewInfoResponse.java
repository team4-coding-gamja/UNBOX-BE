package com.example.unbox_product.common.client.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OrderForReviewInfoResponse {

    private Long buyerId;
    private String buyerNickname;

    private String orderStatus;

    private UUID productId;
    private String productName;
    private String modelNumber;
    private String productImageUrl;

    private UUID productOptionId;
    private String productOptionName;

    private String brandName;
}
