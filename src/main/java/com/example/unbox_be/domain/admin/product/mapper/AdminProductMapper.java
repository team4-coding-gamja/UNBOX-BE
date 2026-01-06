package com.example.unbox_be.domain.admin.product.mapper;

import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductMapper {

    @Mapping(target = "brandId", source = "brand.id")
    AdminProductCreateResponseDto toAdminProductCreateResponseDto(Product product);

    @Mapping(target = "brandId", source = "brand.id")
    AdminProductUpdateResponseDto toAdminProductUpdateResponseDto(Product product);

    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    AdminProductListResponseDto toAdminProductListResponseDto(Product product);
}
