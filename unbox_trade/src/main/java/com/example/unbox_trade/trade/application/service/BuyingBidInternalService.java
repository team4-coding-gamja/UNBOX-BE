package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.trade.application.event.producer.TradeEventProducer;
import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import com.example.unbox_trade.trade.domain.repository.BuyingBidRepository;
import com.example.unbox_trade.trade.presentation.dto.internal.BuyingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.HighestPriceResponseDto;
import com.example.unbox_trade.trade.presentation.mapper.TradeClientMapper;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import com.example.unbox_common.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyingBidInternalService {

    private final BuyingBidRepository buyingBidRepository;
    private final TradeEventProducer tradeEventProducer;
    private final CacheManager cacheManager;
    private final TradeClientMapper tradeClientMapper;
    private final com.example.unbox_trade.common.client.product.ProductClient productClient;

    public static final String UNKNOWN_OPTION_NAME = "Unknown Option";

    // ✅ 구매 글 조회 (주문용) - 캐싱 적용 (읽기 병목 해결 핵심)
    // [Synchronous] 주문 서비스 등에서 상품 정보를 조회할 때 호출 (캐시 적용)
    @Transactional(readOnly = true)
    @Cacheable(value = "trade:bid:buying", key = "#buyingBidId")
    public BuyingBidForOrderInfoResponse getBuyingBidForOrder(UUID buyingBidId) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
        return tradeClientMapper.toBuyingBidForOrderInfoResponse(buyingBid);
    }

    // ✅ 구매 입찰 선점 (주문용: LIVE → RESERVED)
    // [Synchronous] 주문 생성 시 재고 선점을 위해 동기 호출
    @Transactional
    @DistributedLock(key = "#buyingBidId", waitTime = 0)
    public void reserveBuyingBid(UUID buyingBidId, String updatedBy) {
        // 존재 여부 확인
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 동시성 제어: LIVE 상태인 경우에만 RESERVED로 변경
        int updated = buyingBidRepository.updateStatusIfReserved(
                buyingBidId,
                BuyingStatus.LIVE,
                BuyingStatus.RESERVED);

        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // updatedBy 기록
        if (updatedBy != null) {
            BuyingBid refreshed = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                    .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
            refreshed.updateModifiedBy(updatedBy);
        }

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);
    }

    // ✅ 구매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    // [Asynchronous] 결제 완료 후 호출 (이벤트 컨슈머에서 호출 시 비동기, API 호출 시 동기)
    @Transactional
    public void soldBuyingBid(UUID buyingBidId, String updatedBy) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 멱등성: 이미 SOLD 상태면 통과
        if (buyingBid.getStatus() == BuyingStatus.SOLD) {
            log.info("BuyingBid {} is already SOLD. Skipping update.", buyingBidId);
            publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
            evictHighestPriceCache(buyingBid.getProductOptionId());
            evictBuyingBidCache(buyingBidId);
            return;
        }

        if (buyingBid.getStatus() != BuyingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        buyingBid.updateStatus(BuyingStatus.SOLD);
        if (updatedBy != null) {
            buyingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);
    }

    // ✅ 구매 입찰 만료 처리 (주문 취소 시 만료된 경우: RESERVED → CANCELLED)
    // [Asynchronous] 주문 취소/실패 시 호출 (스케줄러나 이벤트 핸들러에서 호출 시 비동기 가능)
    @Transactional
    public void expireBuyingBid(UUID buyingBidId) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (buyingBid.getStatus() == BuyingStatus.SOLD || buyingBid.getStatus() == BuyingStatus.CANCELLED) {
            log.info("BuyingBid {} already terminal state. Skipping.", buyingBidId);
            publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
            evictHighestPriceCache(buyingBid.getProductOptionId());
            evictBuyingBidCache(buyingBidId);
            return;
        }

        if (buyingBid.getStatus() != BuyingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        buyingBid.updateStatus(BuyingStatus.CANCELLED);
        buyingBid.updateModifiedBy("SYSTEM_EXPIRATION");

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);
    }

    // ✅ 구매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    // [Synchronous] 결제 실패나 취소로 인해 다시 매물로 등록될 때 호출
    @Transactional
    public void liveBuyingBid(UUID buyingBidId, String updatedBy) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (buyingBid.getStatus() != BuyingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        buyingBid.updateStatus(BuyingStatus.LIVE);
        if (updatedBy != null) {
            buyingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);
    }

    // ✅ 상품 옵션별 최고가 조회 (Internal)
    @Transactional(readOnly = true)
    @Cacheable(value = "trade:price:highest", key = "#productOptionId", unless = "#result.productOptionName == T(com.example.unbox_trade.trade.application.service.BuyingBidInternalService).UNKNOWN_OPTION_NAME")
    public com.example.unbox_trade.trade.presentation.dto.internal.HighestPriceResponseDto getHighestPrice(
            UUID productOptionId) {
        // 1. 최고가 조회 (LIVE 상태만)
        BigDecimal maxPrice = buyingBidRepository.findHighestPriceByOptionId(productOptionId)
                .orElse(BigDecimal.ZERO);

        // 2. 상품 옵션 정보 조회
        String optionName = UNKNOWN_OPTION_NAME;
        try {
            // Use BuyingBid specific DTO and Client method
            com.example.unbox_trade.common.client.product.dto.ProductOptionForBuyingBidInfoResponse productInfo = productClient
                    .getProductOptionForBuyingBid(productOptionId);
            optionName = productInfo.getProductOptionName();
        } catch (Exception e) {
            log.warn("Product 서비스 호출 실패 - productOptionId: {}, error: {}", productOptionId, e.getMessage());
        }

        return com.example.unbox_trade.trade.presentation.dto.internal.HighestPriceResponseDto.builder()
                .productOptionId(productOptionId)
                .productOptionName(optionName)
                .highestPrice(maxPrice)
                .build();
    }

    @Transactional(readOnly = true)
    public List<HighestPriceResponseDto> getHighestPrices(List<UUID> productOptionIds) {
        if (productOptionIds == null || productOptionIds.isEmpty()) {
            return Collections.emptyList();
        }

        Cache cache = cacheManager.getCache("trade:price:highest");
        List<HighestPriceResponseDto> results = new ArrayList<>();
        List<UUID> missingIds = new ArrayList<>();

        // 1. 캐시에서 먼저 조회
        for (UUID id : productOptionIds) {
            HighestPriceResponseDto cached = (cache != null) ? cache.get(id, HighestPriceResponseDto.class) : null;
            if (cached != null) {
                results.add(cached);
            } else {
                missingIds.add(id);
            }
        }

        // 2. 캐시에 없는 ID들만 한꺼번에 DB 조회
        if (!missingIds.isEmpty()) {
            List<Object[]> dbResults = buyingBidRepository.findHighestPricesByProductOptionIds(missingIds);

            for (Object[] row : dbResults) {
                UUID id = (UUID) row[0];
                BigDecimal price = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;

                HighestPriceResponseDto dto = HighestPriceResponseDto.builder()
                        .productOptionId(id)
                        .productOptionName(null) // Product Service already knows the name
                        .highestPrice(price)
                        .build();

                results.add(dto);

                // 3. DB에서 가져온 데이터는 다음에 사용하기 위해 캐시에 저장
                if (cache != null) {
                    cache.put(id, dto);
                }
            }
        }

        return results;
    }

    // ========================================
    // ✅ Private Helper Methods (Event & Cache)
    // ========================================

    private void evictHighestPriceCache(UUID productOptionId) {
        if (productOptionId != null) {
            Cache cache = cacheManager.getCache("trade:price:highest");
            if (cache != null) {
                cache.evict(productOptionId);
            }
        }
    }

    private void evictBuyingBidCache(UUID buyingId) {
        if (buyingId != null) {
            Cache cache = cacheManager.getCache("trade:bid:buying");
            if (cache != null) {
                cache.evict(buyingId);
            }
        }
    }

    // ✅ Kafka 이벤트 발행 메서드
    private void publishPriceEvent(UUID productId, UUID optionId) {
        // 최고가 조회 (구매 입찰은 최고가가 중요)
        BigDecimal maxPrice = buyingBidRepository.findHighestPriceByOptionId(optionId)
                .orElse(BigDecimal.ZERO);

        TradePriceChangedEvent event = new TradePriceChangedEvent(productId, optionId, maxPrice);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeEventProducer.publishTradePriceChanged(event);
                }
            });
        } else {
            tradeEventProducer.publishTradePriceChanged(event);
        }
    }
}
