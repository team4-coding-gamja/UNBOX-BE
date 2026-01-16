package com.example.unbox_be.common.client.trade.dto;

import com.example.unbox_be.trade.domain.entity.SellingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellingBidForOrderInfoResponse {
    private UUID sellingId;
    private Long sellerId;
    private UUID productOptionId;
    private BigDecimal price;
    private SellingStatus sellingStatus;
}
