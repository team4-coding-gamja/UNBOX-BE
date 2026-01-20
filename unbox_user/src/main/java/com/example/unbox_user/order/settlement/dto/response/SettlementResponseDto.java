package com.example.unbox_user.order.settlement.dto.response;

import com.example.unbox_user.order.settlement.entity.Settlement;
import com.example.unbox_user.order.settlement.entity.SettlementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SettlementResponseDto {
    private UUID settlementId;
    private UUID orderId;
    private BigDecimal totalAmount;
    private Long sellerId;
    private BigDecimal settlementAmount;
    private BigDecimal feesAmount;
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
