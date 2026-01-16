package com.example.unbox_be.trade.infrastructure.adapter;

import com.example.unbox_be.common.client.trade.TradeClient;
import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.trade.domain.repository.SellingBidRepository;
import com.example.unbox_be.trade.presentation.mapper.TradeClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeClientAdapter implements TradeClient {

    private final SellingBidRepository sellingBidRepository;
    private final TradeClientMapper tradeClientMapper;

    // 판매 글 id 조회 (장바구니 용)
    @Override
    public SellingBidForCartInfoResponse getSellingBidForCart(UUID sellingBidId){
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForCartInfoResponse(sellingBid);
    }

    // 판매 글 id 조회 (정버용)
    public SellingBidForCartInfoResponse getSellingBidForOrder(UUID sellingBidId){
        SellingBid sellingBid = sellingBidRepository.findWithDetailsByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForOrderInfoResponse(sellingBid);
    }

}
