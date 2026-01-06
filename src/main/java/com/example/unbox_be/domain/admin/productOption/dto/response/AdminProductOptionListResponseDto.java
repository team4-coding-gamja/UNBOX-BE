package com.example.unbox_be.domain.admin.productOption.dto.response;

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

    private UUID id;
    private String option;

    private UUID productId;
    private String productName;
}
