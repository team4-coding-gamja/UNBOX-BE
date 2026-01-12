package com.example.unbox_be.global.client.order;

import com.example.unbox_be.global.client.order.dto.OrderForReviewInfoResponse;

import java.util.UUID;

public interface OrderClient {

    OrderForReviewInfoResponse getOrderInfo(UUID orderId);
}
