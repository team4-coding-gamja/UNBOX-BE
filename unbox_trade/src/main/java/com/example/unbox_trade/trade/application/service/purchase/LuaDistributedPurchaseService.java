package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LuaDistributedPurchaseService implements PurchaseService {

    private final SellingBidRepository sellingBidRepository;
    private final PurchaseTransactionService purchaseTransactionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BID_OPTION_MAP_PREFIX = "bid:option:";
    private static final String BID_QUEUE_PREFIX = "bids:option:";

    // Lua Script: 리스트의 왼쪽(LPOP)에서 하나를 꺼내서 반환. 없으면 nil 반환.
    private static final String LPOP_SCRIPT = "return redis.call('LPOP', KEYS[1])";

    @Override
    public void purchase(UUID requestBidId, Long buyerId) {
        // 1. [매핑] 요청된 Bid ID를 통해 Option ID를 알아냅니다. (Stage 4와 동일한 캐싱)
        String mapKey = BID_OPTION_MAP_PREFIX + requestBidId;
        Object cachedOptionId = redisTemplate.opsForValue().get(mapKey);
        UUID optionId;

        if (cachedOptionId != null) {
            optionId = UUID.fromString(cachedOptionId.toString());
        } else {
            // 캐시 미스 시 DB 조회 (최초 1회만 발생)
            SellingBid bid = sellingBidRepository.findById(requestBidId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
            optionId = bid.getProductOptionId();
            redisTemplate.opsForValue().set(mapKey, optionId.toString(), 1, TimeUnit.HOURS);
        }

        // 2. Redis Queue에서 입찰 ID 하나 꺼내기 (Atomic)
        // 락(Lock) 없이 원자적으로 실행됩니다.
        String queueKey = BID_QUEUE_PREFIX + optionId;
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>(LPOP_SCRIPT, String.class);
        
        // LPOP 실행
        String matchedBidIdStr = redisTemplate.execute(redisScript, Collections.singletonList(queueKey));

        // 3. 결과 처리
        if (matchedBidIdStr == null) {
            // 큐가 비어있음 = 매물 다 팔림
            // DB를 확인하지 않고 즉시 품절 처리 (Fail-Fast)
            throw new CustomException("구매 가능한 매물이 없습니다.", ErrorCode.BID_ALREADY_MATCHED);
        }

        UUID matchedBidId = UUID.fromString(matchedBidIdStr);
        // log.info("[Stage 5] Matched Bid: {}", matchedBidId); // 성능 위해 주석 처리 추천

        // 4. [낙관적 처리] DB 업데이트
        // Redis에서 내가 유일하게 ID를 선점했으므로, DB 충돌은 없다고 가정하고 바로 결제 진행
        try {
            purchaseTransactionService.decreaseStock(matchedBidId, buyerId);
        } catch (Exception e) {
            // 만약 DB 업데이트 실패 시(예: DB 다운), Redis에서 꺼낸 ID를 다시 큐에 넣는(Rollback) 로직이 필요할 수 있음
            // 하지만 리셀 플랫폼 특성상 '결제 실패'로 처리하고 매물을 살려두는 것이 일반적
            log.error("[Stage 5] Purchase Failed for Bid: {}", matchedBidId, e);
            throw e;
        }
    }
}