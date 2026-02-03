package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidSearchCondition;
import com.example.unbox_trade.trade.presentation.dto.response.AdminBuyingBidListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminBuyingBidService {
    // 구매 입찰 목록 조회
    Page<AdminBuyingBidListResponseDto> getBuyingBids(BuyingBidSearchCondition condition, Pageable pageable);

    // 구매 입찰 삭제
    void deleteBuyingBid(UUID buyingBidId, String deletedBy);

    void deleteBuyingBidsByOptionIds(List<UUID> optionIds, String deletedBy);

    void deleteBuyingBidByOptionId(UUID optionId, String deletedBy);
}
