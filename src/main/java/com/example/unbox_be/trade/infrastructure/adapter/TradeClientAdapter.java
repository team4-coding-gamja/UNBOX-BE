package com.example.unbox_be.trade.infrastructure.adapter;

import com.example.unbox_be.common.client.trade.TradeClient;
import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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

    // ✅ 판매 글 조회 (장바구니용)
    @Override
    public SellingBidForCartInfoResponse getSellingBidForCart(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForCartInfoResponse(sellingBid);
    }

    // ✅ 판매 글 조회 (주문용)
    public SellingBidForOrderInfoResponse getSellingBidForOrder(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForOrderInfoResponse(sellingBid);
    }

    // ✅ 판매 입찰 선점 (LIVE → RESERVED)
    @Transactional
    public void occupySellingBid(UUID sellingBidId) {
        // 1) 존재 여부 확인
        if (!sellingBidRepository.existsById(sellingBidId)) {
            throw new CustomException(ErrorCode.SELLING_BID_NOT_FOUND);
        }

        // 2) 동시성 제어 업데이트 (LIVE 상태인 것만 RESERVED로 변경)
        int updated = sellingBidRepository.updateStatusIfReserved(
                sellingBidId,
                SellingStatus.LIVE,
                SellingStatus.RESERVED);

        // 3) 업데이트 실패 시 예외 발생
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // ✅ 판매 입찰 복구 (RESERVED → LIVE)
    @Transactional
    public void releaseSellingBid(UUID sellingBidId) {
        // 1) 존재 여부 확인
        if (!sellingBidRepository.existsById(sellingBidId)) {
            throw new CustomException(ErrorCode.SELLING_BID_NOT_FOUND);
        }

        // 2) 동시성 제어 업데이트 (RESERVED 상태인 것만 LIVE로 복구)
        int updated = sellingBidRepository.updateStatusIfReserved(
                sellingBidId,
                SellingStatus.RESERVED,
                SellingStatus.LIVE);

        // 3) 업데이트 실패 시 예외 발생
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // ✅ 판매 입찰 상태 변경 (결제용 - RESERVED → SOLD 또는 LIVE)
    @Override
    @Transactional
    public void updateSellingBidStatus(UUID sellingBidId, String status, String updatedBy) {
        // 1) 존재 여부 확인
        if (!sellingBidRepository.existsById(sellingBidId)) {
            throw new CustomException(ErrorCode.SELLING_BID_NOT_FOUND);
        }

        // 2) String → Enum 변환
        SellingStatus newStatus = SellingStatus.valueOf(status);

        // 3) 상태 업데이트 (Repository 직접 사용 또는 엔티티 조회 후 변경)
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        sellingBid.updateStatus(newStatus);
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }
    }
}
