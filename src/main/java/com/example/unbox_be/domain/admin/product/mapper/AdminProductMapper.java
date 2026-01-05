package com.example.unbox_be.domain.admin.product.mapper;

import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductMapper {

    @Mapping(target = "brandId", source = "brand.id")
    AdminProductCreateResponseDto toAdminProductCreateResponseDto(Product product);

    @Mapping(target = "brandId", source = "brand.id")
    AdminProductUpdateResponseDto toAdminProductUpdateResponseDto(Product product);

    @Mapping(target = "optionId", source = "id")
    @Mapping(target = "productId", source = "product.id")
    AdminProductOptionCreateResponseDto toAdminProductOptionCreateResponseDto(ProductOption productOption);
}
