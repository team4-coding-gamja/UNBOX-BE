package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

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

    /**
     * 판매 입찰 선점 (주문용: LIVE → RESERVED)
     */
    void reserveSellingBid(UUID sellingBidId, String updatedBy);
    /**
     * 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
     */
    void soldSellingBid(UUID sellingBidId, String updatedBy);
    /**
     * 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
     */
    void liveSellingBid(UUID sellingBidId, String updatedBy);
}