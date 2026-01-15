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
public class BrandListResponseDto {
    private UUID id;
    private String name;
    private String logoUrl;
}
