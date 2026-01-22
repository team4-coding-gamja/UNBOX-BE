package com.example.unbox_order.settlement.controller;

import com.example.unbox_order.common.client.settlement.dto.SettlementForPaymentResponse;
import com.example.unbox_order.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/settlement")
@RequiredArgsConstructor
public class SettlementInternalController {

    private final SettlementService settlementService;

    // ✅ 정산 조회 (결제용)
    @GetMapping("/{id}/for-payment")
    public SettlementForPaymentResponse getSettlementForPayment(@PathVariable UUID id) {
        return settlementService.getSettlementForPayment(id);
    }
}