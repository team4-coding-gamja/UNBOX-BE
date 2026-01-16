package com.example.unbox_be.product.product.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOptionListResponseDto {

    private UUID  productOptionId;
    private String productOptionName;
//    private Integer lowestPrice;
}
