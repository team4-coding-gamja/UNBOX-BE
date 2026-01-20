package com.example.unbox_product.common.client.order;

import com.example.unbox_product.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface OrderFeignClient {

    @GetMapping("/internal/orders/{orderId}/for-review")
    OrderForReviewInfoResponse getOrderForReview(@PathVariable("orderId") UUID orderId);
}
