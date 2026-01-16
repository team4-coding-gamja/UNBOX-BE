package com.example.unbox_be.product.product.presentation.mapper;

import com.example.unbox_be.product.product.presentation.dto.response.BrandListResponseDto;
import com.example.unbox_be.product.product.domain.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BrandMapper {
    default BrandListResponseDto toBrandListDto(Brand brand) {
        return BrandListResponseDto.builder()
                .brandId(brand.getId())
                .brandName(brand.getName())
                .brandImageUrl(brand.getImageUrl())
                .build();
    }
}
