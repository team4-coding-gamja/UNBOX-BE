package com.example.unbox_be.domain.product.mapper;

import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductRequestMapper {

    @Mapping(target = "id", source = "id")
    ProductRequestResponseDto toProductRequestResponseDto(ProductRequest entity);
}
