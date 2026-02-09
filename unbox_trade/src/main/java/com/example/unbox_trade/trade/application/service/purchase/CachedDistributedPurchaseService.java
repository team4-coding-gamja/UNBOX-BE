package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachedDistributedPurchaseService implements PurchaseService {

    private final RedissonClient redissonClient;
    private final SellingBidRepository sellingBidRepository;
    private final PurchaseTransactionService purchaseTransactionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SOLD_OUT_CACHE_PREFIX = "option:soldout:";
    private static final String BID_OPTION_MAP_PREFIX = "bid:option:";

    @Override
    public void purchase(UUID sellingBidId, Long buyerId) {
        // 1. [Optimization] Bid ID로부터 Option ID를 가져옴 (매핑 캐시 활용)
        String mapKey = BID_OPTION_MAP_PREFIX + sellingBidId;
        Object cachedOptionId = redisTemplate.opsForValue().get(mapKey);
        UUID optionId = null;

        if (cachedOptionId != null) {
            if (cachedOptionId instanceof String) {
                optionId = UUID.fromString((String) cachedOptionId);
            } else if (cachedOptionId instanceof UUID) {
                optionId = (UUID) cachedOptionId;
            }
        }

        if (optionId == null) {
            log.info("[Stage 4] Mapping cache miss for bid: {}", sellingBidId);
            SellingBid bid = sellingBidRepository.findById(sellingBidId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
            optionId = bid.getProductOptionId();
            redisTemplate.opsForValue().set(mapKey, optionId.toString(), 1, TimeUnit.HOURS);
        }

        String soldOutKey = SOLD_OUT_CACHE_PREFIX + optionId;

        // 2. [Gatekeeping] 품절 캐시 확인 (사전 차단 - DB 접근 없음)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(soldOutKey))) {
            log.info("[Stage 4] Cache HIT for option: {}", optionId);
            throw new CustomException("해당 옵션의 모든 매물이 소진되었습니다.", ErrorCode.BID_ALREADY_MATCHED);
        }

        RLock optionLock = redissonClient.getLock("purchase:lock:option:" + optionId);
        
        try {
            log.info("[Stage 4] Attempting lock for option: {}", optionId);
            // 락 대기 시간을 3초로 설정
            boolean available = optionLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!available) {
                log.warn("[Stage 4] Lock timeout for option: {}", optionId);
                // 락 획득 실패 시에도 그사이 품절되었는지 체크하여 캐시 갱신
                if (!sellingBidRepository.existsByProductOptionIdAndStatusAndDeletedAtIsNull(optionId, SellingStatus.LIVE)) {
                    log.info("[Stage 4] Non-lock holder setting sold-out cache for option: {}", optionId);
                    redisTemplate.opsForValue().set(soldOutKey, "TRUE", 10, TimeUnit.MINUTES);
                }
                throw new IllegalStateException("접속자가 많아 처리가 지연되고 있습니다.");
            }

            // 4. 락 획득 후 Double Check
            if (Boolean.TRUE.equals(redisTemplate.hasKey(soldOutKey))) {
                log.info("[Stage 4] Double-check Cache HIT for option: {}", optionId);
                throw new CustomException("이미 품절된 옵션입니다.", ErrorCode.BID_ALREADY_MATCHED);
            }

            // 5. DB에서 현재 가장 저렴한 LIVE 매물 조회 (Next Best)
            SellingBid bestBid = sellingBidRepository.findFirstByProductOptionIdAndStatusAndDeletedAtIsNullOrderByPriceAsc(
                    optionId, SellingStatus.LIVE)
                    .orElse(null);

            if (bestBid == null) {
                log.info("[Stage 4] DB Sold-out, setting cache for option: {}", optionId);
                redisTemplate.opsForValue().set(soldOutKey, "TRUE", 10, TimeUnit.MINUTES);
                throw new CustomException("현재 구매 가능한 매물이 없습니다.", ErrorCode.BID_ALREADY_MATCHED);
            }

            // 6. 매칭 및 상태 변경 트랜잭션 수행
            purchaseTransactionService.decreaseStock(bestBid.getId(), buyerId);
            log.info("[Stage 4] Purchase SUCCESS for bestBid: {} by buyer: {}", bestBid.getId(), buyerId);

            // 7. [Proactive] 마지막 매물이었다면 즉시 캐시 등록
            if (!sellingBidRepository.existsByProductOptionIdAndStatusAndDeletedAtIsNull(optionId, SellingStatus.LIVE)) {
                log.info("[Stage 4] Last item purchased, setting proactive cache for option: {}", optionId);
                redisTemplate.opsForValue().set(soldOutKey, "TRUE", 10, TimeUnit.MINUTES);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("구매 처리 중 오류가 발생했습니다.");
        } finally {
            if (optionLock.isHeldByCurrentThread()) {
                optionLock.unlock();
            }
        }
    }
}
