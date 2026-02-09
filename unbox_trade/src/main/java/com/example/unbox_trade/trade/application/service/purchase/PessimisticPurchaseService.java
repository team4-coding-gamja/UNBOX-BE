package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PessimisticPurchaseService implements PurchaseService {

    private final SellingBidRepository sellingBidRepository;

    @Transactional
    @Override
    public void purchase(UUID sellingBidId, Long buyerId) {
        // 비관적 락을 걸고 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException("이미 판매된 입찰입니다.", ErrorCode.BID_ALREADY_MATCHED);
        }

        // 상태 변경 (구매 체결)
        sellingBid.updateStatus(SellingStatus.SOLD); 
        // 실제로는 Order 생성 등을 해야 하지만 테스트 목적상 상태 변경만 수행
    }
}
