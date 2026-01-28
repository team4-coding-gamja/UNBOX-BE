package com.example.unbox_product.product.presentation.mapper;

import com.example.unbox_product.product.presentation.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_product.product.domain.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductOptionMapper {

    @Mapping(target = "productOptionId", source = "id")
    @Mapping(target = "productOptionName", source = "name")
    @Mapping(target = "productId", source = "product.id")
    AdminProductOptionCreateResponseDto toAdminProductOptionCreateResponseDto(ProductOption productOption);

    @Mapping(target = "productOptionId", source = "id")
    @Mapping(target = "productOptionName", source = "name")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    AdminProductOptionListResponseDto toAdminProductOptionResponseDto(ProductOption productOption);
}
