package com.example.unbox_be.domain.product.mapper;

import com.example.unbox_be.domain.product.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminBrandMapper {

    // ✅ 브랜드 생성
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand);

    // ✅ 브랜드 목록 조회
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandListResponseDto toAdminBrandListResponseDto(Brand brand);

    // ✅ 브랜드 조회
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandDetailResponseDto toAdminBrandDetailResponseDto(Brand brand);

    // ✅ 브랜드 수정
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandUpdateResponseDto toAdminBrandUpdateResponseDto(Brand brand);
}
