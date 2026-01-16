package com.example.unbox_be.trade.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SellingBidsPriceUpdateResponseDto {

    private UUID sellingBidId;
    private BigDecimal newPrice;
}