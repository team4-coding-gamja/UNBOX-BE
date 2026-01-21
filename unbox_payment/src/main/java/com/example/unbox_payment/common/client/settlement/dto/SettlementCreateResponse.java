package com.example.unbox_payment.common.client.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateResponse {
    private UUID settlementId;        // 정산 ID
    private String settlementStatus;  // 정산 상태 (PENDING)
}
