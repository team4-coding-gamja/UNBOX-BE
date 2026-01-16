package com.example.unbox_be.trade.presentation.mapper;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TradeClientMapper {
    SellingBidForCartInfoResponse toSellingBidForCartInfoResponse(SellingBid sellingBid);

    SellingBidForCartInfoResponse toSellingBidForOrderInfoResponse(SellingBid sellingBid);
}
