package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.trade.dto.response.ProductSizePriceResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingStatus; // Enum import 확인
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이므로 readOnly=true (성능 향상)
public class TradeService {

    private final SellingBidRepository sellingBidRepository;

    /**
     * 특정 상품의 사이즈별 최저가 조회 (Public API)
     * - 외부(Product)에서는 이 메서드만 호출합니다.
     * - 내부적으로 'LIVE' 상태인 것만 필터링합니다.
     */

    public List<ProductSizePriceResponseDto> getLowestPriceList(UUID productId) {
        return sellingBidRepository.findLowestPriceByProductId(
                productId,
                SellingStatus.LIVE // 여기서 Enum 상수를 넘겨줍니다.
        );
    }
}