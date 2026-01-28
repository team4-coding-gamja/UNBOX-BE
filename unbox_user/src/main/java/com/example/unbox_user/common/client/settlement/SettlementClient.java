package com.example.unbox_user.common.client.settlement;

import com.example.unbox_user.common.client.settlement.dto.SettlementCreateResponse;
import com.example.unbox_user.common.client.settlement.dto.SettlementForPaymentResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public interface SettlementClient {

    // ✅ 정산 조회 (결제용)
    @GetMapping("/internal/settlement/{id}/for-payment")
    SettlementForPaymentResponse getSettlementForPayment (@PathVariable UUID id);

    // ✅ 정산 생성 (결제용)
    @PostMapping("/internal/settlement")
    SettlementCreateResponse createSettlementForPayment(@RequestParam UUID paymentId);
}
