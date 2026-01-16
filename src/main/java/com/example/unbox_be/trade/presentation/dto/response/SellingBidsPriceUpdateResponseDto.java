package com.example.unbox_be.trade.presentation.dto.response;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    private UUID id;
    private BigDecimal newPrice;
}