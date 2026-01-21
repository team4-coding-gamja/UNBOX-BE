package com.example.unbox_payment.payment.controller;


import com.example.unbox_payment.common.client.payment.dto.PaymentForSettlementResponse;
import com.example.unbox_payment.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class PaymentInternalController {

    private final PaymentService paymentService;

    // ✅ 결제 조회 (정산용)
    @GetMapping("/{id}/for-settlement")
    public PaymentForSettlementResponse getPaymentForSettlement(@PathVariable UUID id) {
        return paymentService.getPaymentForSettlement(id);
    }
}
