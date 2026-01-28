package com.example.unbox_product.common.client.order;

import com.example.unbox_product.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", url = "${order-service.url}", path = "/order")
public interface OrderClient {

    @GetMapping("/internal/orders/{id}/for-review")
    OrderForReviewInfoResponse getOrderForReview(@PathVariable UUID id);
}