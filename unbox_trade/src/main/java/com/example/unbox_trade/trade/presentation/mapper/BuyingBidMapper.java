package com.example.unbox_trade.trade.presentation.mapper;

import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class BuyingBidMapper {

    public BuyingBid toEntity(BuyingBidCreateRequestDto requestDto, Long buyerId, LocalDateTime deadline,
            ProductOptionForSellingBidInfoResponse productInfo) {
        return BuyingBid.builder()
                .buyerId(buyerId)
                .productOptionId(requestDto.getProductOptionId())
                .productId(productInfo.getProductId())
                .price(requestDto.getPrice())
                .deadline(deadline)
                .productName(productInfo.getProductName())
                .modelNumber(productInfo.getModelNumber())
                .productImageUrl(productInfo.getProductImageUrl())
                .productOptionName(productInfo.getProductOptionName())
                .brandName(productInfo.getBrandName())
                .build();
    }

    public BuyingBidCreateResponseDto toCreateResponseDto(BuyingBid buyingBid) {
        return BuyingBidCreateResponseDto.builder()
                .buyingBidId(buyingBid.getId())
                .price(buyingBid.getPrice())
                .createdAt(buyingBid.getCreatedAt())
                .deadline(buyingBid.getDeadline())
                .build();
    }

    public BuyingBidsPriceUpdateResponseDto toPriceUpdateResponseDto(UUID buyingId, BigDecimal newPrice) {
        return BuyingBidsPriceUpdateResponseDto.builder()
                .buyingId(buyingId)
                .updatedPrice(newPrice)
                .build();
    }

    public BuyingBidDetailResponseDto toDetailResponseDto(BuyingBid buyingBid) {
        return BuyingBidDetailResponseDto.builder()
                .buyingId(buyingBid.getId())
                .buyerId(buyingBid.getBuyerId())
                .productId(buyingBid.getProductId())
                .productName(buyingBid.getProductName())
                .modelNumber(buyingBid.getModelNumber())
                .productImageUrl(buyingBid.getProductImageUrl())
                .productOptionName(buyingBid.getProductOptionName())
                .brandName(buyingBid.getBrandName())
                .price(buyingBid.getPrice())
                .status(buyingBid.getStatus())
                .deadline(buyingBid.getDeadline())
                .createdAt(buyingBid.getCreatedAt())
                .build();
    }

    public BuyingBidListResponseDto toListResponseDto(BuyingBid buyingBid) {
        return BuyingBidListResponseDto.builder()
                .buyingId(buyingBid.getId())
                .productName(buyingBid.getProductName())
                .productOptionName(buyingBid.getProductOptionName())
                .productImageUrl(buyingBid.getProductImageUrl())
                .price(buyingBid.getPrice())
                .status(buyingBid.getStatus())
                .createdAt(buyingBid.getCreatedAt())
                .deadline(buyingBid.getDeadline())
                .build();
    }
}
