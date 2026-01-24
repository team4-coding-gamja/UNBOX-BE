package com.example.unbox_trade.trade.presentation.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowestPriceResponseDto implements Serializable {
    private UUID productOptionId;
    private String productOptionName;
    private BigDecimal lowestPrice;
}
