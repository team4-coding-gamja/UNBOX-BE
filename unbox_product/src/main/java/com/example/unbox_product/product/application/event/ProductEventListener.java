package com.example.unbox_product.product.application.event;

import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ✅ 현재: Spring 내부 이벤트 리스너
     * @TransactionalEventListener: Trade의 트랜잭션이 성공(Commit)했을 때만 실행됨 (Kafka의 안정성과 유사)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePriceChange(TradePriceChangedEvent event) {
        log.info("🔔 [Internal Event] Price Changed: {} -> {}", event.productId(), event.newLowestPrice());

        // Redis 업데이트 로직 (이전과 동일)
        String key = "prod:price:" + event.productId();
        redisTemplate.opsForValue().set(key, event.newLowestPrice());
    }
}