package com.example.unbox_be.trade.presentation.dto.request;

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
@AllArgsConstructor
@Builder
public class SellingBidCreateRequestDto {

    @NotNull(message = "option ID 없음.")
    private UUID productOptionId;

    @NotNull(message = "Price 값 없음.")
    @Positive(message = "가격은 0보다 커야함")
    private BigDecimal price;
}
