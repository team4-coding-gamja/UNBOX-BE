package com.example.unbox_be.domain.admin.product.mapper;

import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.staff.dto.response.*;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;

public class AdminProductMapper {

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
