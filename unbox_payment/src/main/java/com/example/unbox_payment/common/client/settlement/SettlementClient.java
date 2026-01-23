package com.example.unbox_payment.common.client.settlement;

import com.example.unbox_payment.common.client.settlement.dto.SettlementCreateResponse;
import com.example.unbox_payment.common.client.settlement.dto.SettlementForPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "unbox-order", contextId = "settlementClient", url = "${order-service.url}")
public interface SettlementClient {

    // ✅ 정산 조회 (결제용)
    @GetMapping("/internal/settlement/{id}/for-payment")
    SettlementForPaymentResponse getSettlementForPayment (@PathVariable("id") UUID id);

    // ✅ 정산 생성 (결제용)
    @PostMapping("/internal/settlement/create")
    SettlementCreateResponse createSettlementForPayment(@RequestParam("paymentId") UUID paymentId);
}
