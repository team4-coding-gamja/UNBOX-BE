package com.example.unbox_user.common.client.payment;

import com.example.unbox_user.common.client.payment.dto.PaymentForSettlementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

public interface PaymentClient {

    // ✅ 결제 조회 (정산용)
    @GetMapping("/internal/payment/{id}/for-settlement")
    PaymentForSettlementResponse getPaymentForSettlement (@PathVariable UUID id);
}
