package com.example.unbox_be.domain.product.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminBrandCreateResponseDto {

    private UUID id;
    private String name;
    private String logoUrl;
}
