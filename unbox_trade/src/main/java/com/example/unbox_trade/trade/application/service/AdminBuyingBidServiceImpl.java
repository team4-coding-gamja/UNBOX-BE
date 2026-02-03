package com.example.unbox_trade.trade.application.service;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.repository.AdminBuyingBidRepository;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidSearchCondition;
import com.example.unbox_trade.trade.presentation.dto.response.AdminBuyingBidListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminBuyingBidServiceImpl implements AdminBuyingBidService {

    private final AdminBuyingBidRepository buyingBidRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminBuyingBidListResponseDto> getBuyingBids(BuyingBidSearchCondition condition, Pageable pageable) {

        Page<BuyingBid> buyingBids = buyingBidRepository.findAdminBuyingBids(condition, pageable);

        return buyingBids.map(bid -> AdminBuyingBidListResponseDto.builder()
                .buyingBidId(bid.getId())
                .status(bid.getStatus())
                .price(bid.getPrice())
                .deadline(bid.getDeadline())
                .createdAt(bid.getCreatedAt())
                .updatedAt(bid.getUpdatedAt())
                .buyerId(bid.getBuyerId())
                .productOptionId(bid.getProductOptionId())
                .productOptionName(bid.getProductOptionName())
                .productId(bid.getProductId())
                .productName(bid.getProductName())
                .brandId(null) // BrandID not stored in BuyingBid
                .brandName(bid.getBrandName())
                .modifiedBy(bid.getUpdatedBy()) // UpdatedBy from BaseEntity
                .build());
    }

    @Override
    @Transactional
    public void deleteBuyingBid(UUID buyingBidId, String deletedBy) {
        BuyingBid buyingBid = buyingBidRepository.findById(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        buyingBid.softDelete(deletedBy);
    }

    // [Bulk Delete]
    @Override
    @Transactional
    public void deleteBuyingBidsByOptionIds(List<UUID> optionIds, String deletedBy) {
        if (optionIds == null || optionIds.isEmpty()) {
            return;
        }
        buyingBidRepository.softDeleteByOptionIds(optionIds, deletedBy);
    }

    // [Bulk Delete]
    @Override
    @Transactional
    public void deleteBuyingBidByOptionId(UUID optionId, String deletedBy) {
        buyingBidRepository.softDeleteByOptionId(optionId, deletedBy);
    }
}
