package com.example.unbox_be.product.product.mapper;

import com.example.unbox_be.product.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.product.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.product.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.product.product.entity.Product;
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
