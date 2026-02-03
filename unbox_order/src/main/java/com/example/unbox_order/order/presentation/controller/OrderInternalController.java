package com.example.unbox_order.order.presentation.controller;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.order.application.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[내부] 주문 관리", description = "내부 시스템용 주문 API")
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderInternalController {

    private final OrderService orderService;

    @Operation(summary = "주문 조회 (리뷰용)", description = "리뷰 작성을 위한 주문 정보를 조회합니다.")
    @GetMapping("/{id}/for-review")
    public OrderForReviewInfoResponse getOrderForReview(@PathVariable UUID id) {
        return orderService.getOrderForReview(id);
    }

    @Operation(summary = "주문 조회 (결제용)", description = "결제 처리를 위한 주문 정보를 조회합니다.")
    @GetMapping("/{id}/for-payment")
    public OrderForPaymentInfoResponse getOrderForPayment(@PathVariable UUID id) {
        return orderService.getOrderForPayment(id);
    }

    @Operation(summary = "결제 완료 처리", description = "결제 완료 후 주문 상태를 PENDING_SHIPMENT로 변경합니다.")
    @PostMapping("/{id}/pending-shipment")
    public void pendingShipmentOrder(
            @PathVariable UUID id,
            @RequestParam UUID paymentId,
            @RequestParam String updatedBy) {
        orderService.pendingShipmentOrder(id, paymentId, updatedBy);
    }
}
