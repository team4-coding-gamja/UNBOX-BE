package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.trade.presentation.dto.request.SellingBidSearchCondition;
import com.example.unbox_trade.trade.presentation.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_trade.trade.domain.repository.AdminSellingBidRepository;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSellingBidServiceImpl implements AdminSellingBidService {

    private final AdminSellingBidRepository sellingBidRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminSellingBidListResponseDto> getSellingBids(SellingBidSearchCondition condition, Pageable pageable) {

        Page<SellingBid> sellingBids = sellingBidRepository.findAdminSellingBids(condition, pageable);

        return sellingBids.map(bid -> {
            // Snapshot 활용 (N+1 문제 해결)
            return AdminSellingBidListResponseDto.builder()
                    .sellingBidId(bid.getId())
                    .status(bid.getStatus())
                    .price(bid.getPrice())
                    .deadline(bid.getDeadline())
                    .createdAt(bid.getCreatedAt())
                    .sellerId(bid.getSellerId())
                    // 상품 옵션 정보 (스냅샷)
                    .productOptionId(bid.getProductOptionId())
                    .productOptionName(bid.getProductOptionName())
                    // 상품 정보 (스냅샷)
                    .productId(bid.getProductId())
                    .productName(bid.getProductName())
                    // 브랜드 정보 (스냅샷)
                    .brandName(bid.getBrandName())
                    .build();
        });
    }

    @Override
    @Transactional
    public void deleteSellingBid(UUID sellingBidId, String deletedBy) {
        SellingBid sellingBid = sellingBidRepository.findById(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        sellingBid.softDelete(deletedBy);
    }

    // [수정] 옵션 ID 리스트로 판매 입찰 일괄 Soft Delete
    @Transactional
    public void deleteSellingBidsByOptionIds(List<UUID> optionIds, String deletedBy) {
        if (optionIds == null || optionIds.isEmpty()) {
            return; // 빈 리스트면 실행하지 않음 (SQL 에러 방지)
        }
        sellingBidRepository.softDeleteByOptionIds(optionIds, deletedBy);
    }

    // [수정] 단건 옵션 ID로 판매 입찰 Soft Delete
    @Transactional
    public void deleteSellingBidByOptionId(UUID optionId, String deletedBy) {
        sellingBidRepository.softDeleteByOptionId(optionId, deletedBy);
    }
}
