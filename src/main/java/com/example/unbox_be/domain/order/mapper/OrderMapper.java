package com.example.unbox_be.domain.order.mapper;

import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "brandName", target = "brandName")
    @Mapping(source = "productName", target = "productName")
    @Mapping(source = "optionName", target = "size")
    @Mapping(source = "imageUrl", target = "imageUrl")
    OrderResponseDto toResponseDto(Order order);

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = ".", target = "productOption")
    @Mapping(source = ".", target = "product")
    OrderDetailResponseDto toDetailResponseDto(Order order);

    @Mapping(source = "productOptionId", target = "id")
    @Mapping(source = "optionName", target = "size")
    OrderDetailResponseDto.ProductOptionInfo toProductOptionInfo(Order order);

    @Mapping(source = "productId", target = "id")
    @Mapping(source = "brandName", target = "brandName")
    @Mapping(source = "productName", target = "name")
    @Mapping(source = "modelNumber", target = "modelNumber")
    @Mapping(source = "imageUrl", target = "imageUrl")
    OrderDetailResponseDto.ProductInfo toProductInfo(Order order);
}