package com.example.unbox_be.domain.admin.brand.mapper;

import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.product.entity.Brand;

public class AdminBrandMapper {

    public static AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand) {
        return AdminBrandCreateResponseDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .build();
    }
}
