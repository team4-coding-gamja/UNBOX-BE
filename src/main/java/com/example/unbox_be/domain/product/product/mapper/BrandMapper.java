package com.example.unbox_be.domain.product.product.mapper;

import com.example.unbox_be.domain.product.product.dto.response.BrandListResponseDto;
import com.example.unbox_be.domain.product.product.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BrandMapper {
    default BrandListResponseDto toBrandListDto(Brand brand) {
        return BrandListResponseDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .build();
    }
}
