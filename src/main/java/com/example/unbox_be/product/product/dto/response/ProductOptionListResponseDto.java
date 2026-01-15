package com.example.unbox_be.product.product.dto.response;

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

    private UUID  id;
    private String productOptionName;
    private Integer lowestPrice;
}
