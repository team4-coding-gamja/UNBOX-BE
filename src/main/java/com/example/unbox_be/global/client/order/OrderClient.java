package com.example.unbox_be.global.client.order;

import com.example.unbox_be.global.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// @FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/internal/orders/{id}/for-review")
    OrderForReviewInfoResponse getOrderInfo(@PathVariable UUID id);
}
