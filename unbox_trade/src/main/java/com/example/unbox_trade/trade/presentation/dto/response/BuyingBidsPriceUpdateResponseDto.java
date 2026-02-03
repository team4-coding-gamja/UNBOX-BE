package com.example.unbox_trade.trade.presentation.dto.response;

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
public class BuyingBidsPriceUpdateResponseDto {
    private UUID buyingId;
    private BigDecimal updatedPrice;
}
