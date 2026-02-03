package com.example.unbox_trade.trade.application.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
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
    public void cancelBuyingBid(UUID buyingId, Long userId, String deleteBy) {
        log.info("Cancelling buying bid: buyingBidId={}, userId={}", buyingId, userId);

        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingId)
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

        log.info("Successfully cancelled buying bid: buyingBidId={}, userId={}", buyingId, userId);

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingId);
    }

    // ✅ 구매 입찰 수정
    @Transactional
    public BuyingBidsPriceUpdateResponseDto updateBuyingBidPrice(UUID buyingId,
            BuyingBidsPriceUpdateRequestDto requestDto, Long userId) {
        log.info("Updating buying bid price: buyingBidId={}, userId={}, newPrice={}",
                buyingId, userId, requestDto.getNewPrice());

        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingId)
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
                buyingId, oldPrice, requestDto.getNewPrice());

        // 🔔 가격 갱신 이벤트 & 캐시 무효화
        publishPriceEvent(buyingBid.getProductId(), buyingBid.getProductOptionId());
        evictHighestPriceCache(buyingBid.getProductOptionId());
        evictBuyingBidCache(buyingId);

        return buyingBidMapper.toPriceUpdateResponseDto(buyingId, requestDto.getNewPrice());
    }

    // ✅ 구매 입찰 상세 조회
    @Transactional(readOnly = true)
    public BuyingBidDetailResponseDto getBuyingBidDetail(UUID buyingId, Long userId) {
        BuyingBid buyingBid = buyingBidRepository.findByIdAndDeletedAtIsNull(buyingId)
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
