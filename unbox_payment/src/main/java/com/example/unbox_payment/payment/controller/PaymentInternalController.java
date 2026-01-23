package com.example.unbox_payment.payment.controller;


import com.example.unbox_payment.payment.dto.internal.PaymentForSettlementResponse;
import com.example.unbox_payment.payment.dto.internal.PaymentStatusResponse;
import com.example.unbox_payment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@Tag(name = "[내부] 결제 관리", description = "내부 시스템용 결제 API")
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentService paymentService;

    // ✅ 결제 조회 (정산용)
    @Operation(summary = "정산용 결제 정보 조회", description = "정산 처리를 위해 결제 정보를 조회합니다.")
    @GetMapping("/{id}/for-settlement")
    public PaymentForSettlementResponse getPaymentForSettlement(@PathVariable UUID id) {
        return paymentService.getPaymentForSettlement(id);
    }

    // ✅ 결제 상태 조회
    @Operation(summary = "결제 상태 조회", description = "주문 ID로 결제 상태를 조회합니다.")
    @GetMapping("/orders/{orderId}/status")
    public PaymentStatusResponse getPaymentStatus(@PathVariable UUID orderId) {
        return paymentService.getPaymentStatus(orderId);
    }
}
