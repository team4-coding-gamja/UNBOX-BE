package com.example.unbox_be.domain.admin.brand.mapper;

import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdminBrandMapper {

    // ✅ 브랜드 생성 응답
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandCreateResponseDto toAdminBrandCreateResponseDto(Brand brand);

    // ✅ 브랜드 목록 조회
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandListResponseDto toAdminBrandListResponseDto(Brand brand);

    // ✅ 브랜드 수정 응답
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "logoUrl", source = "logoUrl")
    AdminBrandUpdateResponseDto toAdminBrandUpdateResponseDto(Brand brand);
}
