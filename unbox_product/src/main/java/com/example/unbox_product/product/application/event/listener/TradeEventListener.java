package com.example.unbox_product.product.application.event.listener;

import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeEventListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "trade-events", groupId = "product-group")
    @Transactional
    public void handleTradeEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, Object> record, Acknowledgment ack){
        Object event = record.value();

        if (event == null) {
            log.warn("[Trade->Product] Received null event. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        String eventType = event.getClass().getSimpleName();
        log.info("[Trade->Product] Received Event: {}", eventType);

        try{
            if(event instanceof TradePriceChangedEvent tradePriceChangedEvent){
                log.info("🔔 [Internal Event] Price Changed: {} -> {}", tradePriceChangedEvent.productId(), tradePriceChangedEvent.price());

                // Redis 업데이트 로직 (이전과 동일)
                String key = "product:prices:" + tradePriceChangedEvent.productId();

                redisTemplate.opsForHash().put(
                        key,
                        tradePriceChangedEvent.optionId().toString(), // Field (옵션 ID)
                        tradePriceChangedEvent.price().toString()  // Value (가격)
                );

            }
            else {
                log.warn("Ignored unknown event type: {}", eventType);
                // 모르는 이벤트라도 처리는 된 것이므로 ack를 할지 말지는 정책 결정 필요.
                // 보통은 에러를 내지 않고 넘어가야 컨슈머가 멈추지 않음.
            }

            ack.acknowledge();
        } catch (Exception e){
            log.error("Failed to process event: {}", eventType, e);
            // 여기서 예외를 던져야 Kafka 설정에 따라 재시도(Retry)를 하거나 DLQ로 빠짐
            throw e;
        }

    }

}