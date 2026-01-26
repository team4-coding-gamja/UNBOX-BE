package com.example.unbox_order.order.controller;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.order.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@Tag(name = "[내부] 주문 관리", description = "내부 시스템용 주문 API")
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    // ✅ 주문 조회 (리뷰용)
    @Operation(summary = "주문 조회 (리뷰용)", description = "리뷰 서비스에서 주문 정보를 조회합니다.")
    @GetMapping("/{id}/for-review")
    public OrderForReviewInfoResponse getOrderForReview(@PathVariable UUID id) {
        return orderService.getOrderForReview(id);
    }

    // ✅ 주문 조회 (결제용)
    @Operation(summary = "주문 조회 (결제용)", description = "결제 서비스에서 주문 정보를 조회합니다.")
    @GetMapping("/{id}/for-payment")
    public OrderForPaymentInfoResponse getOrderForPayment(@PathVariable UUID id) {
        return orderService.getOrderForPayment(id);
    }

    // ✅ 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
    @Operation(summary = "주문 상태 변경 (발송 대기)", description = "결제 완료 후 주문 상태를 발송 대기로 변경합니다.")
    @PostMapping("{id}/pending-shipment")
    public ResponseEntity<Void> pendingShipmentOrder (@PathVariable UUID id, @RequestParam String updatedBy) {
        orderService.pendingShipmentOrder(id, updatedBy);
        return ResponseEntity.ok().build();
    }
}