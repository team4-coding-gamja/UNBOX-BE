package com.example.unbox_order.settlement.controller;

import com.example.unbox_order.common.client.settlement.dto.SettlementForPaymentResponse;
import com.example.unbox_order.common.client.settlement.dto.SettlementCreateResponse;
import com.example.unbox_order.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

@Tag(name = "[내부] 정산 관리", description = "내부 시스템용 정산 API")
@RestController
@RequestMapping("/internal/settlement")
@RequiredArgsConstructor
public class SettlementInternalController {

    private final SettlementService settlementService;

    // 정산 조회 (결제용)
    @Operation(summary = "정산 조회 (결제용)", description = "정산 ID로 정산 정보를 조회합니다.")
    @GetMapping("/{id}/for-payment")
    public SettlementForPaymentResponse getSettlementForPayment(@PathVariable UUID id) {
        return settlementService.getSettlementForPayment(id);
    }
    // 정산 생성 (결제용)
    @Operation(summary = "정산 생성", description = "결제 ID 기반으로 새로운 정산 데이터를 생성합니다.")
    @PostMapping("/create")
    public SettlementCreateResponse createSettlementForPayment(@RequestParam UUID paymentId) {
        return settlementService.createSettlementForPayment(paymentId);
    }
}