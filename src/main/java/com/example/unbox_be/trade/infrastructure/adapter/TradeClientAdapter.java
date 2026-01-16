package com.example.unbox_be.trade.infrastructure.adapter;

import com.example.unbox_be.common.client.trade.TradeClient;
import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.trade.domain.entity.SellingStatus;
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

    // 판매 글 id 조회 (주문용)
    public SellingBidForOrderInfoResponse getSellingBidForOrder(UUID sellingBidId){
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForOrderInfoResponse(sellingBid);
    }

    @Transactional
    public void occupySellingBid(UUID sellingBidId) {
        // 1. 존재 여부 확인 (혹은 findById로 가져와도 됨)
        // 존재하지 않는 ID라면 여기서 1차로 걸러짐
        if (!sellingBidRepository.existsById(sellingBidId)) {
            throw new CustomException(ErrorCode.SELLING_BID_NOT_FOUND);
        }

        // 2. 동시성 제어 업데이트 (LIVE 상태인 것만 MATCHED로 변경)
        // updateStatusIfMatch는 Repository에 정의된 @Modifying 쿼리여야 함
        // Status 변경해야함 (MATCHED -> )
        int updated = sellingBidRepository.updateStatusIfMatch(
                sellingBidId,
                SellingStatus.LIVE,
                SellingStatus.MATCHED
        );

        // 3. 업데이트 실패 시 (이미 팔렸거나, 상태가 LIVE가 아님) 예외 발생
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

}

