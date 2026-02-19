package com.example.unbox_order.order.presentation.controller;

import com.example.unbox_order.order.application.service.OrderService;
import com.example.unbox_order.order.presentation.dto.internal.OrderForPaymentInfoResponse;
import com.example.unbox_order.order.presentation.dto.internal.OrderForReviewInfoResponse;
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
    public OrderForPaymentInfoResponse getOrderForPayment(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            @RequestHeader(value = "X-Fault-Delay-MS", required = false) Long faultDelay) {

        // [장애 주입] 지연 발생 (target=order)
        if ("order".equalsIgnoreCase(faultTarget)) {
            try {
                long delay = (faultDelay != null) ? faultDelay : 3000;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return orderService.getOrderForPayment(id);
    }

    @Operation(summary = "결제 완료 처리", description = "결제 완료 후 주문 상태를 PENDING_SHIPMENT로 변경합니다.")
    @PostMapping("/{id}/pending-shipment")
    public void pendingShipmentOrder(
            @PathVariable UUID id,
            @RequestParam UUID paymentId,
            @RequestParam String updatedBy,
            @RequestHeader(value = "X-Test-Mode", required = false) String testMode,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            @RequestHeader(value = "X-Fault-Delay-MS", required = false) Long faultDelay) {

        // [장애 주입] 지연 발생 (target=order)
        if ("order".equalsIgnoreCase(faultTarget)) {
            try {
                long delay = (faultDelay != null) ? faultDelay : 3000;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        orderService.pendingShipmentOrder(id, paymentId, updatedBy, testMode);
    }
}
