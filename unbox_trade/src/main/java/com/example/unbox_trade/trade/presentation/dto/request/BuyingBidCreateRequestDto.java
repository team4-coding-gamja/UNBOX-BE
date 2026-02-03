package com.example.unbox_trade.trade.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class BuyingBidCreateRequestDto {
    @NotNull
    private UUID productOptionId;

    @NotNull
    @Positive
    private BigDecimal price;
}
