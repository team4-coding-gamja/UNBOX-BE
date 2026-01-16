package com.example.unbox_be.product.product.presentation.mapper;

import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.product.product.domain.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminBrandMapper {

    // ✅ 브랜드 생성
    @Mapping(target = "brandId", source = "id")
    @Mapping(target = "brandName", source = "name")
    @Mapping(target = "brandImageUrl", source = "imageUrl")
    AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand);

    // ✅ 브랜드 목록 조회
    @Mapping(target = "brandId", source = "id")
    @Mapping(target = "brandName", source = "name")
    @Mapping(target = "brandImageUrl", source = "imageUrl")
    AdminBrandListResponseDto toAdminBrandListResponseDto(Brand brand);

    // ✅ 브랜드 조회
    @Mapping(target = "brandId", source = "id")
    @Mapping(target = "brandName", source = "name")
    @Mapping(target = "brandImageUrl", source = "imageUrl")
    AdminBrandDetailResponseDto toAdminBrandDetailResponseDto(Brand brand);

    // ✅ 브랜드 수정
    @Mapping(target = "brandId", source = "id")
    @Mapping(target = "brandName", source = "name")
    @Mapping(target = "brandImageUrl", source = "imageUrl")
    AdminBrandUpdateResponseDto toAdminBrandUpdateResponseDto(Brand brand);
}
