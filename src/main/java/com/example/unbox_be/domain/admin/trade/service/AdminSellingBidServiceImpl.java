package com.example.unbox_be.domain.admin.trade.service;

import com.example.unbox_be.domain.admin.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.domain.admin.trade.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_be.domain.admin.trade.repository.AdminSellingBidRepository;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminSellingBidServiceImpl implements AdminSellingBidService {

    private final AdminSellingBidRepository sellingBidRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminSellingBidListResponseDto> getSellingBids(SellingBidSearchCondition condition, Pageable pageable) {
        // Repository 조회 (검색 조건 적용)
        Page<SellingBid> sellingBids = sellingBidRepository.findAdminSellingBids(condition, pageable);

        // DTO 변환 및 반환
        return sellingBids.map(bid -> AdminSellingBidListResponseDto.builder()
                .id(bid.getId())
                .productName(bid.getProductOption().getProduct().getName())
                .brandName(bid.getProductOption().getProduct().getBrand().getName())
                .size(bid.getProductOption().getOption())
                .price(bid.getPrice())
                .status(bid.getStatus())
                .createdAt(bid.getCreatedAt())
                .deadline(bid.getDeadline())
                .build());
    }

    @Override
    @Transactional
    public void deleteSellingBid(UUID sellingId, String deletedBy) {
        SellingBid sellingBid = sellingBidRepository.findById(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        sellingBid.softDelete(deletedBy);
    }
}
