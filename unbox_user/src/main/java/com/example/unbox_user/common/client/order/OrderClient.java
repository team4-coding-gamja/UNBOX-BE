package com.example.unbox_user.common.client.order;

import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// @FeignClient(name = "trade-service")
public interface OrderClient {

    @GetMapping("/internal/order/{id}/for-review")
    OrderForReviewInfoResponse getOrderForReview (@PathVariable UUID id);
}
