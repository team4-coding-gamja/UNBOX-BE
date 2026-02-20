package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseTransactionService {

    private final SellingBidRepository sellingBidRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(UUID sellingBidId, Long buyerId) {
        SellingBid sellingBid = sellingBidRepository.findById(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException("이미 판매된 입찰입니다.", ErrorCode.BID_ALREADY_MATCHED);
        }

        sellingBid.updateStatus(SellingStatus.SOLD);
    }
}
