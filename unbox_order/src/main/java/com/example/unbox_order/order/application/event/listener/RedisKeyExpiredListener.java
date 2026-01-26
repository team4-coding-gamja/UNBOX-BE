package com.example.unbox_order.order.application.event.listener;

import com.example.unbox_common.event.order.OrderExpiredEvent;
import com.example.unbox_order.order.application.event.producer.OrderEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

    private final OrderEventProducer orderEventProducer;
    private static final String REDIS_ORDER_KEY_PREFIX = "order:expiration:";

    public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer, OrderEventProducer orderEventProducer) {
        super(listenerContainer);
        this.orderEventProducer = orderEventProducer;
    }

    /**
     * Redis 키 만료 이벤트 수신
     * 만료된 키가 "order:expiration:{orderId}:{sellingBidId}" 형식인지 확인하고 Kafka 이벤트 발행
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        if (expiredKey.startsWith(REDIS_ORDER_KEY_PREFIX)) {
            log.info("Redis Key Expired: {}", expiredKey);
            handleOrderExpired(expiredKey);
        }
    }

    private void handleOrderExpired(String expiredKey) {
        try {
            // 키 형식: order:expiration:{orderId}:{sellingBidId}
            // Value는 만료되면 사라지므로, 필요한 정보(sellingBidId)를 Key에 포함시켜야 함!
            String[] parts = expiredKey.split(":");
            if (parts.length != 4) {
                log.warn("Invalid expired key format: {}", expiredKey);
                return;
            }

            UUID orderId = UUID.fromString(parts[2]);
            UUID sellingBidId = UUID.fromString(parts[3]);

            log.info("Triggering OrderExpiredEvent for Order: {}, SellingBid: {}", orderId, sellingBidId);
            
            // Kafka 이벤트 발행
            OrderExpiredEvent event = OrderExpiredEvent.of(orderId, sellingBidId);
            orderEventProducer.publishOrderExpired(event);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in expired key: {}", expiredKey, e);
        } catch (Exception e) {
            log.error("Failed to handle expired key: {}", expiredKey, e);
        }
    }
}
