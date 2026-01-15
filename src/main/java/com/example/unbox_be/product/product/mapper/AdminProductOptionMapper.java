package com.example.unbox_be.product.product.mapper;

import com.example.unbox_be.product.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.product.product.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_be.product.product.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductOptionMapper {

    @Mapping(target = "productId", source = "product.id")
    AdminProductOptionCreateResponseDto toAdminProductOptionCreateResponseDto(ProductOption productOption);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    AdminProductOptionListResponseDto toAdminProductOptionResponseDto(ProductOption productOption);
}
