package com.example.unbox_be.domain.trade.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSizePriceResponseDto {
    private String size;    // 사이즈 이름 (예: "260")
    private Integer price;  // 최저가
}