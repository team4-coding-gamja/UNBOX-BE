package com.example.unbox_be.trade.presentation.mapper;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TradeClientMapper {


    default SellingBidForCartInfoResponse toSellingBidForCartInfoResponse(SellingBid sellingBid){
        return SellingBidForCartInfoResponse.builder()
                .sellingId(sellingBid.getId())
                .sellerId(sellingBid.getSellerId())
                .productOptionId(sellingBid.getProductOptionId())
                .price(sellingBid.getPrice())
                .sellingStatus(sellingBid.getStatus())
                .productId(sellingBid.getProductId())
                .productName(sellingBid.getProductName())
                .productOptionName(sellingBid.getProductOptionName())
                .productImageUrl(sellingBid.getProductImageUrl())
                .modelNumber(sellingBid.getModelNumber())
                .brandName(sellingBid.getBrandName())
                .brandId(sellingBid.getBrandId())
                .build();
    }

    default SellingBidForOrderInfoResponse toSellingBidForOrderInfoResponse(SellingBid sellingBid){
        return SellingBidForOrderInfoResponse.builder()
                .sellingId(sellingBid.getId())
                .sellerId(sellingBid.getSellerId())
                .productOptionId(sellingBid.getProductOptionId())
                .price(sellingBid.getPrice())
                .sellingStatus(sellingBid.getStatus())
                .productId(sellingBid.getProductId())
                .productName(sellingBid.getProductName())
                .productOptionName(sellingBid.getProductOptionName())
                .productImageUrl(sellingBid.getProductImageUrl())
                .modelNumber(sellingBid.getModelNumber())
                .brandName(sellingBid.getBrandName())
                .brandId(sellingBid.getBrandId())
                .build();
    }
}
