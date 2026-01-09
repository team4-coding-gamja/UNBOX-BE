package com.example.unbox_be.domain.product.mapper;

import com.example.unbox_be.domain.product.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AdminProductRequestMapper {

    AdminProductRequestListResponseDto toListResponseDto(ProductRequest productRequest);

    AdminProductRequestUpdateResponseDto toUpdateResponseDto(ProductRequest productRequest);
}
