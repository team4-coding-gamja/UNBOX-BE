package com.example.unbox_be.trade.application.service;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.trade.domain.entity.SellingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface SellingBidService {

    /**
     * 판매 입찰 생성
     */
    SellingBidCreateResponseDto createSellingBid(Long sellerId, SellingBidCreateRequestDto requestDto);

    /**
     * 판매 입찰 취소
     */
    void cancelSellingBid(UUID sellingId, Long userId, String deletedBy);

    /**
     * 판매 입찰 가격 수정
     */
    SellingBidsPriceUpdateResponseDto updateSellingBidPrice(UUID sellingId, SellingBidsPriceUpdateRequestDto requestDto, Long userId
    );
    /**
     * 판매 입찰 단건 조회
     */
    SellingBidDetailResponseDto getSellingBidDetail(UUID sellingId, Long userId);

    /**
     * 내 판매 입찰 목록 조회
     */
    Slice<SellingBidListResponseDto> getMySellingBids(Long userId, Pageable pageable);

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================
    SellingBidForCartInfoResponse getSellingBidForCart(UUID sellingBidId);

    SellingBidForOrderInfoResponse getSellingBidForOrder(UUID sellingBidId);

    void occupySellingBid(UUID sellingBidId);

    void updateSellingBidStatus(UUID id, String status, String updatedBy);
}