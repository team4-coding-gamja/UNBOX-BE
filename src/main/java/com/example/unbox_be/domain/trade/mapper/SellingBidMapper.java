package com.example.unbox_be.domain.trade.mapper;

import com.example.unbox_be.domain.trade.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.global.client.product.dto.ProductOptionForSellingBidInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SellingBidMapper {

        @Mapping(target = "id", ignore = true)
        @Mapping(target = "sellerId", source = "sellerId")
        @Mapping(target = "productOptionId", source = "productInfo.id")
        @Mapping(target = "price", source = "dto.price")
        @Mapping(target = "status", ignore = true)
        @Mapping(target = "deadline", source = "deadline")

        @Mapping(target = "productName", source = "productInfo.productName")
        @Mapping(target = "modelNumber", source = "productInfo.modelNumber")
        @Mapping(target = "productImageUrl", source = "productInfo.productImageUrl")
        @Mapping(target = "productOptionName", source = "productInfo.productOptionName")

        SellingBid toEntity(
                        SellingBidCreateRequestDto dto,
                        Long sellerId,
                        LocalDateTime deadline,
                        ProductOptionForSellingBidInfoResponse productInfo);

        @Mapping(source = "id", target = "id")
        SellingBidCreateResponseDto toCreateResponseDto(SellingBid entity);

        SellingBidsPriceUpdateResponseDto toPriceUpdateResponseDto(UUID id, BigDecimal newPrice);

        @Mapping(source = "entity.id", target = "id")
        @Mapping(source = "entity.status", target = "status")
        @Mapping(source = "entity.price", target = "price")
        @Mapping(source = "entity.deadline", target = "deadline")
        @Mapping(source = "entity.sellerId", target = "sellerId")
        @Mapping(source = "entity.createdAt", target = "createdAt")
        @Mapping(source = "productInfo.productName", target = "productName")
        @Mapping(source = "productInfo.modelNumber", target = "modelNumber")
        @Mapping(source = "productInfo.productImageUrl", target = "productImageUrl")
        @Mapping(source = "productInfo.productOptionName", target = "productOptionName")
        SellingBidDetailResponseDto toDetailResponseDto(SellingBid entity, ProductOptionForSellingBidInfoResponse productInfo);

        /*
         * =========================
         * List 조회
         * =========================
         */

        @Mapping(source = "entity.id", target = "id")
        @Mapping(source = "entity.status", target = "status")
        @Mapping(source = "entity.price", target = "price")
        @Mapping(source = "entity.deadline", target = "deadline")
        @Mapping(source = "entity.sellerId", target = "sellerId")
        @Mapping(source = "entity.createdAt", target = "createdAt")
        @Mapping(source = "productInfo.productName", target = "productName")
        @Mapping(source = "productInfo.modelNumber", target = "modelNumber")
        @Mapping(source = "productInfo.productImageUrl", target = "productImageUrl")
        @Mapping(source = "productInfo.productOptionName", target = "productOptionName")
        SellingBidListResponseDto toListResponseDto(SellingBid entity, ProductOptionForSellingBidInfoResponse productInfo);
}