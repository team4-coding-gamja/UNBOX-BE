package com.example.unbox_be.product.product.presentation.mapper;

import com.example.unbox_be.product.product.presentation.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.product.product.domain.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductMapper {

    @Mapping(target = "productId", source = "id")
    @Mapping(target = "productName", source = "name")
    @Mapping(target = "modelNumber", source = "modelNumber")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    @Mapping(target = "brandId", source = "brand.id")
    AdminProductCreateResponseDto toAdminProductCreateResponseDto(Product product);

    @Mapping(target = "productId", source = "id")
    @Mapping(target = "productName", source = "name")
    @Mapping(target = "modelNumber", source = "modelNumber")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    @Mapping(target = "brandId", source = "brand.id")
    AdminProductUpdateResponseDto toAdminProductUpdateResponseDto(Product product);

    @Mapping(target = "productId", source = "id")
    @Mapping(target = "productName", source = "name")
    @Mapping(target = "modelNumber", source = "modelNumber")
    @Mapping(target = "productImageUrl", source = "imageUrl")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    AdminProductListResponseDto toAdminProductListResponseDto(Product product);
}
