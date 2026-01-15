package com.example.unbox_be.domain.trade.trade.service;

import com.example.unbox_be.domain.trade.trade.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.domain.trade.trade.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.domain.trade.trade.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.domain.trade.trade.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.domain.trade.trade.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.domain.trade.trade.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_be.domain.trade.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.trade.entity.SellingStatus;
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

    /**
     * 판매 입찰 상태 변경 (유저용)
     */
    void updateSellingBidStatus(UUID sellingId, SellingStatus newStatus, Long userId, String email);

    /**
     * 판매 입찰 상태 변경 (시스템용)
     */
    void updateSellingBidStatusBySystem(UUID sellingId, SellingStatus newStatus, String email);

    /**
     * 판매 입찰 엔티티 조회 (내부 시스템용)
     */
    SellingBid findSellingBidById(UUID sellingId);
}