package com.example.unbox_be.domain.admin.mapper;

import com.example.unbox_be.domain.admin.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminMeResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;

public class AdminMapper {

    public static AdminMeResponseDto toAdminMeResponseDto(Admin admin) {
        return AdminMeResponseDto.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nickname(admin.getNickname())
                .phone(admin.getPhone())
                .adminRole(admin.getAdminRole())
                .adminStatus(admin.getAdminStatus())
                .build();
    }

    public static AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand) {
        return AdminBrandCreateResponseDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .build();
    }

    public static AdminProductCreateResponseDto toAdminProductCreateResponseDto(Product product) {
        return AdminProductCreateResponseDto.builder()
                .brandId(product.getBrand().getId())
                .id(product.getId())
                .name(product.getName())
                .modelNumber(product.getModelNumber())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .build();
    }

    public static AdminProductOptionCreateResponseDto toAdminProductOptionCreateResponseDto(ProductOption productOption) {
        return AdminProductOptionCreateResponseDto.builder()
                .id(productOption.getId())
                .productId(productOption.getProduct().getId())
                .option(productOption.getOption())
                .build();
    }
}
