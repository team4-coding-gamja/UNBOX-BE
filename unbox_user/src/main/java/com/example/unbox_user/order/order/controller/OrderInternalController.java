package com.example.unbox_user.order.order.controller;

import com.example.unbox_user.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_user.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_user.order.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    // ✅ 주문 조회 (리뷰용)
    @GetMapping("/{id}/for-review")
    public OrderForReviewInfoResponse getOrderForReview(@PathVariable UUID id) {
        return orderService.getOrderForReview(id);
    }

    // ✅ 주문 조회 (결제용)
    @GetMapping("/{id}/for-payment")
    public OrderForPaymentInfoResponse getOrderForPayment(@PathVariable UUID id) {
        return orderService.getOrderForPayment(id);
    }

    // ✅ 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
    @PostMapping("/internal/order/{id}/pending-shipment")
    public ResponseEntity<Void> pendingShipmentOrder (@PathVariable UUID id, @RequestParam String updatedBy) {
        orderService.pendingShipmentOrder(id, updatedBy);
        return ResponseEntity.ok().build();
    }
}