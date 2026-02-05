package com.example.unbox_user.common.client.order;

import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

public interface OrderClient {

    // 주문 조회 (리뷰용)
    @GetMapping("/internal/order/{id}/for-review")
    OrderForReviewInfoResponse getOrderForReview (@PathVariable UUID id);
}
