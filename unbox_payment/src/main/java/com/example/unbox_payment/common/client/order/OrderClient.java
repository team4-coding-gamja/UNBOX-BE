package com.example.unbox_payment.common.client.order;

import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_payment.common.client.order.dto.OrderForReviewInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "unbox-user", contextId = "orderClient", path = "/api/order")
public interface OrderClient {

    // 주문 조회 (리뷰용)
    @GetMapping("/internal/order/{id}/for-review")
    OrderForReviewInfoResponse getOrderForReview (@PathVariable("id") UUID id);

    // 주문 조회 (결제용)
    @GetMapping("/internal/order/{id}/for-payment")
    OrderForPaymentInfoResponse getOrderForPayment (@PathVariable("id") UUID id);

    // 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
    @PostMapping("/internal/order/{id}/pending-shipment")
    void pendingShipmentOrder (@PathVariable("id") UUID id, @RequestParam("updatedBy") String updatedBy);
}
