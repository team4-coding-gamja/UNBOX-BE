package com.example.unbox_be.domain.admin.brand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBrandListResponseDto {

    private UUID id;
    private String name;
    private String logoUrl;
}

