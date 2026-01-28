package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.common.client.product.ProductClient;
import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_trade.common.client.user.UserClient;
import com.example.unbox_trade.trade.application.event.producer.TradeEventProducer;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.internal.LowestPriceResponseDto;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.presentation.mapper.SellingBidMapper;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import com.example.unbox_trade.trade.presentation.mapper.TradeClientMapper;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import com.example.unbox_common.lock.DistributedLock;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellingBidServiceImpl implements SellingBidService {

    private final SellingBidRepository sellingBidRepository;
    private final SellingBidMapper sellingBidMapper;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final TradeClientMapper tradeClientMapper;
    // private final ApplicationEventPublisher eventPublisher;
    private final TradeEventProducer tradeEventProducer;
    private final CacheManager cacheManager;

    // --- Helper Methods for Cache Eviction ---
    private void evictLowestPriceCache(UUID productOptionId) {
        if (productOptionId != null) {
            Cache cache = cacheManager.getCache("trade:price:lowest");
            if (cache != null) {
                cache.evict(productOptionId);
            }
        }
    }

    private void evictSellingBidCache(UUID sellingBidId) {
        if (sellingBidId != null) {
            Cache cache = cacheManager.getCache("trade:bid:order");
            if (cache != null) {
                cache.evict(sellingBidId);
            }
        }
    }
    // -----------------------------------------

    // ✅ 판매 입찰 생성
    @Override
    @Transactional
    public SellingBidCreateResponseDto createSellingBid(Long sellerId, SellingBidCreateRequestDto requestDto) {
        // 1) 회원 검증 (API Call)
        userClient.getUserInfoForSellingBid(sellerId);

        // 가격 유효성 검사
        if (requestDto.getPrice() == null || requestDto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        ProductOptionForSellingBidInfoResponse productInfo = productClient.getProductOptionForSellingBid(requestDto.getProductOptionId());

        // 만료일(deadline) 30일 뒤 00시로 설정
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        SellingBid sellingBid = sellingBidMapper.toEntity(requestDto, sellerId, deadline, productInfo);

        SellingBid savedBid = sellingBidRepository.save(sellingBid);

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(savedBid.getProductId(), savedBid.getProductOptionId());
        evictLowestPriceCache(savedBid.getProductOptionId());

        return sellingBidMapper.toCreateResponseDto(savedBid);
    }

    // ✅ 판매 입찰 취소
    @Override
    @Transactional
    public void cancelSellingBid(UUID sellingId, Long userId, String deletedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // LIVE 상태만 취소 가능
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 상태 변경
        sellingBid.updateStatus(SellingStatus.CANCELLED);
        if (deletedBy != null) {
            sellingBid.updateModifiedBy(deletedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingId);
    }

    // ✅ 판매 입찰 가격 수정
    @Override
    @Transactional
    public SellingBidsPriceUpdateResponseDto updateSellingBidPrice(UUID sellingId,
            SellingBidsPriceUpdateRequestDto requestDto, Long userId) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 가격 유효성 검사
        if (requestDto.getNewPrice() == null || requestDto.getNewPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // LIVE 상태만 가격 변경 가능
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 엔티티 가격 업데이트 (JPA dirty checking으로 반영)
        sellingBid.updatePrice(requestDto.getNewPrice(), userId, "SYSTEM");

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingId);

        return sellingBidMapper.toPriceUpdateResponseDto(sellingId, requestDto.getNewPrice());
    }

    // ✅ 판매 입찰 상세 조회
    @Override
    @Transactional(readOnly = true)
    public SellingBidDetailResponseDto getSellingBidDetail(UUID sellingId, Long userId) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // Product 서비스 호출
        ProductOptionForSellingBidInfoResponse productInfo = productClient
                .getProductOptionForSellingBid(sellingBid.getProductOptionId());

        return sellingBidMapper.toDetailResponseDto(sellingBid, productInfo);
    }

    // ✅ 내 판매 입찰 목록 조회 (Slice)
    @Override
    @Transactional(readOnly = true)
    public Slice<SellingBidListResponseDto> getMySellingBids(Long userId, Pageable pageable) {

        Slice<SellingBid> bids = sellingBidRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable);

        return bids.map(bid -> {
            ProductOptionForSellingBidInfoResponse productInfo = productClient
                    .getProductOptionForSellingBid(bid.getProductOptionId());

            return sellingBidMapper.toListResponseDto(bid, productInfo);
        });
    }

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    // ✅ 판매 글 조회 (장바구니용)
    @Override
    @Transactional(readOnly = true)
    public SellingBidForCartInfoResponse getSellingBidForCart(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForCartInfoResponse(sellingBid);
    }

    // ✅ 판매 글 조회 (주문용) - 캐싱 적용 (읽기 병목 해결 핵심)
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trade:bid:order", key = "#sellingBidId")
    public SellingBidForOrderInfoResponse getSellingBidForOrder(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForOrderInfoResponse(sellingBid);
    }

    // ✅ 판매 입찰 선점 (주문용: LIVE → RESERVED)
    @Override
    @Transactional
    @DistributedLock(key = "#sellingBidId", waitTime = 0)
    public void reserveSellingBid(UUID sellingBidId, String updatedBy) {
        // 존재 여부 확인
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        
        // 동시성 제어 업데이트 (LIVE 상태인 것만 RESERVED로 변경)
        int updated = sellingBidRepository.updateStatusIfReserved(
                sellingBidId,
                SellingStatus.LIVE,
                SellingStatus.RESERVED);
        
        // 업데이트 실패 시 예외 발생
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // updatedBy 기록
        if (updatedBy != null) {
            SellingBid refreshed = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
            refreshed.updateModifiedBy(updatedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화 (상태가 변했으니 최저가도 변했을 수 있음)
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingBidId);
    }

    // ✅ 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @Transactional
    @Override
    public void soldSellingBid(UUID sellingBidId, String updatedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // 멱등성 보장: 이미 SOLD 상태라면 캐시/이벤트만 갱신하고 종료
        if (sellingBid.getStatus() == SellingStatus.SOLD) {
            log.info("SellingBid {} is already SOLD. Refreshing cache/events and skipping update.", sellingBidId);
            publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
            evictLowestPriceCache(sellingBid.getProductOptionId());
            evictSellingBidCache(sellingBidId);
            return;
        }

        // 상태 검증 (RESERVED 상태만 SOLD로 변경 가능)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 상태 변경
        sellingBid.updateStatus(SellingStatus.SOLD);
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingBidId);
    }

    // ✅ 판매 입찰 만료 처리 (주문 취소 시 만료된 경우: RESERVED → CANCELLED)
    @Transactional
    @Override
    public void expireSellingBid(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));

        // 이미 완료/취소된 건은 무시 (멱등성) 지만 캐시/이벤트 갱신은 수행
        if (sellingBid.getStatus() == SellingStatus.SOLD || sellingBid.getStatus() == SellingStatus.CANCELLED) {
            log.info("SellingBid {} already in terminal state ({}). Refreshing cache/events only.",
                    sellingBidId, sellingBid.getStatus());
            publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
            evictLowestPriceCache(sellingBid.getProductOptionId());
            evictSellingBidCache(sellingBidId);
            return;
        }

        // 상태 검증 (RESERVED 상태만 만료 처리 가능)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            log.warn("SellingBid {} is in {} state, cannot expire. Only RESERVED bids can be expired.",
                    sellingBidId, sellingBid.getStatus());
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 상태 변경
        sellingBid.updateStatus(SellingStatus.CANCELLED); // 혹은 EXPIRED 상태가 별도로 있다면 그것 사용
        sellingBid.updateModifiedBy("SYSTEM_EXPIRATION");
        
        log.info("Expired SellingBid {} due to timeout.", sellingBidId);

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingBidId);
    }

    // ----------------------------------------------------
    // ✅ Kafka 이벤트 발행 메서드 (수정됨)
    // ----------------------------------------------------

    private void publishPriceEvent(UUID productId, UUID optionId) {
        // 쿼리는 트랜잭션 내에서 수행 (데이터 일관성 유지)
        BigDecimal minPrice = sellingBidRepository.findLowestPriceByOptionId(optionId)
                .orElse(BigDecimal.ZERO);

        TradePriceChangedEvent event = new TradePriceChangedEvent(productId, optionId, minPrice);

        // Kafka 발행은 트랜잭션 커밋이 성공한 직후에 수행
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tradeEventProducer.publishTradePriceChanged(event);
                }
            });
        } else {
            // 트랜잭션이 없는 경우 즉시 발행
            tradeEventProducer.publishTradePriceChanged(event);
        }
    }

    // ✅ 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @Override
    @Transactional
    public void liveSellingBid(UUID sellingBidId, String updatedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // 상태 검증 (RESERVED 상태만 LIVE로 복구 가능)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 상태 변경
        sellingBid.updateStatus(SellingStatus.LIVE);
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행 & 캐시 무효화
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
        evictLowestPriceCache(sellingBid.getProductOptionId());
        evictSellingBidCache(sellingBidId);
    }

    public static final String UNKNOWN_OPTION_NAME = "Unknown Option";

    // ✅ 상품 옵션별 최저가 조회 (Internal) - 캐싱 적용!
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trade:price:lowest", key = "#productOptionId", unless = "#result.productOptionName == T(com.example.unbox_trade.trade.application.service.SellingBidServiceImpl).UNKNOWN_OPTION_NAME")
    public LowestPriceResponseDto getLowestPrice(UUID productOptionId) {
        // 1. 최저가 조회 (LIVE 상태만)
        BigDecimal minPrice = sellingBidRepository.findLowestPriceByOptionId(productOptionId)
                .orElse(BigDecimal.ZERO);

        // 2. 상품 옵션 정보 조회 (이름이 필요함)
        String optionName = UNKNOWN_OPTION_NAME;
        try {
            ProductOptionForSellingBidInfoResponse productInfo = productClient.getProductOptionForSellingBid(productOptionId);
            optionName = productInfo.getProductOptionName();
        } catch (Exception e) {
            log.warn("Product 서비스 호출 실패 - productOptionId: {}, error: {}", productOptionId, e.getMessage());
        }

        return LowestPriceResponseDto.builder()
                .productOptionId(productOptionId)
                .productOptionName(optionName)
                .lowestPrice(minPrice)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<LowestPriceResponseDto> getLowestPrices(List<UUID> productOptionIds) {
        if (productOptionIds == null || productOptionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> results = sellingBidRepository.findLowestPricesByProductOptionIds(productOptionIds);

        return results.stream()
                .map(row -> LowestPriceResponseDto.builder()
                        .productOptionId((UUID) row[0])
                        .productOptionName(null) // Product Service already knows the name
                        .lowestPrice(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO)
                        .build())
                .toList();
    }
}