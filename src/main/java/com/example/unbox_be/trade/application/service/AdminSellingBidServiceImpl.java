package com.example.unbox_be.trade.application.service;

import com.example.unbox_be.product.product.infrastructure.adapter.ProductClientAdapter;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.trade.presentation.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_be.trade.domain.repository.AdminSellingBidRepository;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
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
    private final ProductClientAdapter productClientAdapter;

    // Todo : N + 1 문제 발생
    @Override
    @Transactional(readOnly = true)
    public Page<AdminSellingBidListResponseDto> getSellingBids(SellingBidSearchCondition condition, Pageable pageable) {

        Page<SellingBid> sellingBids = sellingBidRepository.findAdminSellingBids(condition, pageable);

        return sellingBids.map(bid -> {
            // ✅ Product 서비스로부터 상세 정보 조회
            ProductOptionForSellingBidInfoResponse productInfo = productClientAdapter.getProductOptionForSellingBid(bid.getProductOptionId());

            // ✅ 설계하신 AdminSellingBidListResponseDto 구조에 맞게 매핑
            return AdminSellingBidListResponseDto.builder()
                    .id(bid.getId())
                    .status(bid.getStatus())
                    .price(bid.getPrice())
                    .deadline(bid.getDeadline())
                    .createdAt(bid.getCreatedAt())
                    .sellerId(bid.getSellerId())
                    // 상품 옵션 정보
                    .productOptionId(bid.getProductOptionId())
                    .productOptionName(productInfo.getProductOptionName())
                    // 상품 정보
                    .productId(productInfo.getProductId())
                    .productName(productInfo.getProductName())
                    // 브랜드 정보
                    .brandId(productInfo.getBrandId())
                    .brandName(productInfo.getBrandName())
                    .build();
        });
    }

    @Override
    @Transactional
    public void deleteSellingBid(UUID sellingId, String deletedBy) {
        SellingBid sellingBid = sellingBidRepository.findById(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        sellingBid.softDelete(deletedBy);
    }

    // ✅ [추가] 옵션 ID 리스트로 판매 입찰 일괄 삭제 (브랜드/상품 삭제 시 사용)
    @Transactional
    public void deleteSellingBidsByOptionIds(List<UUID> optionIds) {
        sellingBidRepository.deleteAllByProductOptionIdIn(optionIds);
    }

    // ✅ [추가] 단건 옵션 ID로 판매 입찰 삭제 (옵션 삭제 시 사용)
    @Transactional
    public void deleteSellingBidByOptionId(UUID optionId) {
        sellingBidRepository.deleteByProductOptionId(optionId);
    }
}
