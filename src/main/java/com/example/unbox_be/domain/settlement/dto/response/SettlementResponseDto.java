package com.example.unbox_be.domain.settlement.dto.response;

import com.example.unbox_be.domain.settlement.entity.Settlement;
import com.example.unbox_be.domain.settlement.entity.SettlementStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SettlementResponseDto {
    private UUID settlementId;
    private UUID orderId;
    private Integer totalAmount;
    private Long sellerId;
    private Integer settlementAmount;
    private Integer feesAmount;
    private SettlementStatus settlementStatus;

    public static SettlementResponseDto from(Settlement settlement) {
        return SettlementResponseDto.builder()
                .settlementId(settlement.getId())
                .orderId(settlement.getOrderId())
                .sellerId(settlement.getSellerId())
                .totalAmount(settlement.getTotalAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .feesAmount(settlement.getFeesAmount())
                .settlementStatus(settlement.getSettlementStatus())
                .build();
    }
}
