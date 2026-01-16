package com.example.unbox_be.product.product.presentation.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBrandUpdateResponseDto {

    private UUID brandId;
    private String brandName;
    private String brandImageUrl;
}
