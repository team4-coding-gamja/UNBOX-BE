package com.example.unbox_trade.trade.presentation.mapper;

import com.example.unbox_trade.trade.presentation.dto.internal.BuyingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TradeClientMapper {

    default SellingBidForCartInfoResponse toSellingBidForCartInfoResponse(SellingBid sellingBid) {
        return SellingBidForCartInfoResponse.builder()
                .sellingId(sellingBid.getId())
                .sellerId(sellingBid.getSellerId())
                .productOptionId(sellingBid.getProductOptionId())
                .price(sellingBid.getPrice())
                .sellingStatus(sellingBid.getStatus().name())
                .productId(sellingBid.getProductId())
                .productName(sellingBid.getProductName())
                .productOptionName(sellingBid.getProductOptionName())
                .productImageUrl(sellingBid.getProductImageUrl())
                .modelNumber(sellingBid.getModelNumber())
                .brandName(sellingBid.getBrandName())
                .build();
    }

    default SellingBidForOrderInfoResponse toSellingBidForOrderInfoResponse(SellingBid sellingBid) {
        return SellingBidForOrderInfoResponse.builder()
                .sellingBidId(sellingBid.getId())
                .sellerId(sellingBid.getSellerId())
                .productOptionId(sellingBid.getProductOptionId())
                .price(sellingBid.getPrice())
                .status(sellingBid.getStatus().name())
                .productId(sellingBid.getProductId())
                .productName(sellingBid.getProductName())
                .productOptionName(sellingBid.getProductOptionName())
                .productImageUrl(sellingBid.getProductImageUrl())
                .modelNumber(sellingBid.getModelNumber())
                .brandName(sellingBid.getBrandName())
                .build();
    }

    default BuyingBidForOrderInfoResponse toBuyingBidForOrderInfoResponse(BuyingBid buyingBid) {
        return BuyingBidForOrderInfoResponse.builder()
                .buyingBidId(buyingBid.getId())
                .buyerId(buyingBid.getBuyerId())
                .sellerId(buyingBid.getSellerId())
                .productOptionId(buyingBid.getProductOptionId())
                .price(buyingBid.getPrice())
                .buyingStatus(buyingBid.getStatus().name())
                .productId(buyingBid.getProductId())
                .productName(buyingBid.getProductName())
                .productOptionName(buyingBid.getProductOptionName())
                .productImageUrl(buyingBid.getProductImageUrl())
                .modelNumber(buyingBid.getModelNumber())
                .brandName(buyingBid.getBrandName())
                .build();
    }
}
