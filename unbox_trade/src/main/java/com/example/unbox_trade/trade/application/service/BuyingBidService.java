package com.example.unbox_trade.trade.application.service;

import com.example.unbox_common.event.trade.BuyingBidMatchedEvent;
import com.example.unbox_trade.common.client.product.ProductClient;
import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_trade.trade.application.event.producer.TradeEventProducer;
import com.example.unbox_trade.trade.domain.entity.BuyingBid;
import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import com.example.unbox_trade.trade.domain.repository.BuyingBidRepository;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.*;
import com.example.unbox_trade.trade.presentation.mapper.BuyingBidMapper;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyingBidService {

    private final BuyingBidRepository buyingBidRepository;
    private final BuyingBidMapper buyingBidMapper;
    private final ProductClient productClient;
    private final TradeEventProducer tradeEventProducer;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // ✅ 구매 입찰 생성
    @Transactional
    public BuyingBidCreateResponseDto createBuyingBid(Long userId, BuyingBidCreateRequestDto requestDto) {
        log.info("Creating buying bid for userId={}, productOptionId={}, price={}",
                userId, requestDto.getProductOptionId(), requestDto.getPrice());

        ProductOptionForSellingBidInfoResponse productInfo = productClient
                .getProductOptionForBuyingBid(requestDto.getProductOptionId());

        // Default deadline: 30 days
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        BuyingBid buyingBid = buyingBidMapper.toEntity(requestDto, userId, deadline, productInfo);
        BuyingBid savedBid = buyingBidRepository.save(buyingBid);

        log.info("Successfully created buying bid: buyingBidId={}, userId={}, price={}",
                savedBid.getId(), userId, savedBid.getPrice());

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(savedBid.getProductId(), savedBid.getProductOptionId());
        evictHighestPriceCache(savedBid.getProductOptionId());

        return buyingBidMapper.toCreateResponseDto(savedBid);
    }

    // ✅ 구매 입찰 취소
    @Transactional
    public void cancelBuyingBid(UUID buyingBidId, Long userId, String deleteBy) {
        log.info("Cancelling buying bid: buyingBidId={}, userId={}", buyingBidId, userId);

        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (!Objects.equals(buyingBid.getBuyerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (buyingBid.getStatus() != BuyingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        buyingBid.updateStatus(BuyingStatus.CANCELLED);
        if (deleteBy != null) {
            buyingBid.updateModifiedBy(deleteBy);
        }

        log.info("Successfully cancelled buying bid: buyingBidId={}, userId={}", buyingBidId, userId);

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);
    }

    // ✅ 구매 입찰 수정
    @Transactional
    public BuyingBidsPriceUpdateResponseDto updateBuyingBidPrice(UUID buyingBidId,
            BuyingBidsPriceUpdateRequestDto requestDto, Long userId) {
        log.info("Updating buying bid price: buyingBidId={}, userId={}, newPrice={}",
                buyingBidId, userId, requestDto.getNewPrice());

        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (!Objects.equals(buyingBid.getBuyerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (buyingBid.getStatus() != BuyingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        BigDecimal oldPrice = buyingBid.getPrice();
        buyingBid.updatePrice(requestDto.getNewPrice(), userId, "SYSTEM");

        log.info("Successfully updated buying bid price: buyingBidId={}, oldPrice={}, newPrice={}",
                buyingBidId, oldPrice, requestDto.getNewPrice());

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingBidId);

        return buyingBidMapper.toPriceUpdateResponseDto(buyingBidId, requestDto.getNewPrice());
    }

    // ✅ 구매 입찰 상세 조회
    @Transactional(readOnly = true)
    public BuyingBidDetailResponseDto getBuyingBidDetail(UUID buyingBidId, Long userId) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        if (!Objects.equals(buyingBid.getBuyerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return buyingBidMapper.toDetailResponseDto(buyingBid);
    }

    // ✅ 내 구매 입찰 전체 조회
    @Transactional(readOnly = true)
    public Slice<BuyingBidListResponseDto> getMyBuyingBids(Long userId, Pageable pageable) {
        Slice<BuyingBid> bids = buyingBidRepository.findByBuyerIdOrderByCreatedAtDesc(userId, pageable);
        return bids.map(buyingBidMapper::toListResponseDto);
    }

    // ✅ 판매자 매칭
    @Transactional
    public void matchBuyingBid(UUID buyingBidId, Long sellerId) {

        // 1. 구매 입찰 조회 (비관적 락)
        BuyingBid buyingBid = buyingBidRepository.findByIdWithLock(buyingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 2. 자기 자신의 입찰 수락 방지
        if (Objects.equals(buyingBid.getBuyerId(), sellerId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 3. 상태 검증 (LIVE만 매칭 가능)
        if (buyingBid.getStatus() != BuyingStatus.LIVE) {
            throw new CustomException(ErrorCode.BID_ALREADY_MATCHED);
        }

        // 4. 판매자 매칭 처리
        buyingBid.matchWithSeller(sellerId);

        // 🔔 캐시 무효화 (상태 변경 반영)
        evictBuyingBidCache(buyingBidId);

        log.info("BuyingBid {} matched with Seller {}", buyingBidId, sellerId);

        // 5. 구매자에게 알림 발송 (Kafka 이벤트)
        BuyingBidMatchedEvent event = new BuyingBidMatchedEvent(
                buyingBid.getId(),
                buyingBid.getBuyerId(),
                buyingBid.getSellerId(),
                buyingBid.getProductName(),
                buyingBid.getProductOptionName(),
                buyingBid.getPrice(),
                buyingBid.getMatchedAt());

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeEventProducer.publishBuyingBidMatched(event);
                    log.info("Published BuyingBidMatchedEvent: buyingBidId={}", buyingBid.getId());
                }
            });
        } else {
            tradeEventProducer.publishBuyingBidMatched(event);
            log.info("Published BuyingBidMatchedEvent: buyingBidId={}", buyingBid.getId());
        }

        // 6. 타임아웃 타이머 설정 (Redis)
        String timeoutKey = "buying-bid:match-timeout:" + buyingBidId;

        try {
            // 24시간 TTL 설정
            Boolean result = redisTemplate.opsForValue().setIfAbsent(
                    timeoutKey,
                    "MATCHED",
                    Duration.ofHours(24));

            if (!Boolean.TRUE.equals(result)) {
                log.warn("Match timeout key already exists for BuyingBid: {} - possible duplicate match attempt", buyingBidId);
            }

            log.info("Match timeout set for BuyingBid {}: 24 hours (1440 minutes)", buyingBidId);
        } catch (Exception e) {
            log.error("Failed to set match timeout for BuyingBid: {}", buyingBidId, e);
        }
    }

    // ========================================
    // ✅ Private Helper Methods (Event & Cache)
    // ========================================

    // --- Helper Methods for Cache Eviction (Highest Price) ---
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

        // TODO: TradePriceChangedEvent가 '최저가'만 담는지, '최고가'도 담을 수 있는지 확인 필요
        // 현재는 기존 SellingBidService와 동일하게 TradePriceChangedEvent 사용 가정.
        // 만약 HighestPriceChangedEvent가 필요하다면 클래스 추가 필요.
        // 우선은 tradeEventProducer를 통해 "가격 변경" 사실을 알리는 데 집중.

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
