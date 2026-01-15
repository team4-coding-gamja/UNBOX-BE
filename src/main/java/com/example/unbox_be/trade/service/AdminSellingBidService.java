package com.example.unbox_be.trade.service;

import com.example.unbox_be.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.trade.dto.response.AdminSellingBidListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminSellingBidService {
    // 판매 입찰 목록 조회
    Page<AdminSellingBidListResponseDto> getSellingBids(SellingBidSearchCondition condition, Pageable pageable);

    // 판매 입찰 삭제
    void deleteSellingBid(UUID sellingId, String deletedBy);

    void deleteSellingBidsByOptionIds(List<UUID> optionIds);

    void deleteSellingBidByOptionId(UUID optionId);
}
