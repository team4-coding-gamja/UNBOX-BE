package com.example.unbox_be.product.product.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminProductOptionListResponseDto {

    private UUID productOptionId;
    private String productOptionName;

    private UUID productId;
    private String productName;
}
