package com.example.unbox_be.domain.trade.trade.dto.response;

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
public class SellingBidCreateResponseDto {

    private UUID id;
    private BigDecimal price;

    private String productName;
    private String productOptionName;

    private Long sellerId;
}
