package com.example.unbox_be.domain.trade.trade.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class SellingBidsPriceUpdateRequestDto {

    @NotNull(message = "수정할 가격을 입력해주세요.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal newPrice;
}