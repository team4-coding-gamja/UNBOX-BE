package com.example.unbox_order.order.application.event.listener;

import com.example.unbox_common.event.order.OrderExpiredEvent;
import com.example.unbox_order.common.client.trade.TradeClient;
import com.example.unbox_order.order.application.event.producer.OrderEventProducer;
import com.example.unbox_order.order.application.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

    private final OrderEventProducer orderEventProducer;
    private final TradeClient tradeClient;
    private final OrderService orderService;

    private static final String REDIS_ORDER_KEY_PREFIX = "order:expiration:";
    private static final String REDIS_SHIPMENT_KEY_PREFIX = "order:shipment-deadline:";

    public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer,
            OrderEventProducer orderEventProducer,
            TradeClient tradeClient,
            OrderService orderService) {
        super(listenerContainer);
        this.orderEventProducer = orderEventProducer;
        this.tradeClient = tradeClient;
        this.orderService = orderService;
        // AWS ElastiCache에서는 CONFIG 명령어가 비활성화되어 있으므로 건너뜀
        // ElastiCache 파라미터 그룹에서 notify-keyspace-events = "Ex" 로 직접 설정 필요
        setKeyspaceNotificationsConfigParameter("");
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

        // 배송 기한 만료 처리 (추가된 로직)
        else if (expiredKey.startsWith(REDIS_SHIPMENT_KEY_PREFIX)) {
            log.info("Redis Shipment Key Expired: {}", expiredKey);
            handleShipmentExpired(expiredKey);
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
            String type = parts[3]; // "SELLING" or "BUYING"
            UUID bidId = UUID.fromString(parts[4]);

            log.info("Triggering OrderExpiredEvent for Order: {}, Type: {}, Bid: {}", orderId, type, bidId);

            OrderExpiredEvent event;
            if ("BUYING".equals(type)) {
                event = OrderExpiredEvent.ofBuying(orderId, bidId);
            } else {
                event = OrderExpiredEvent.ofSelling(orderId, bidId);
            }
            orderEventProducer.publishOrderExpired(event);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in expired key: {}", expiredKey, e);
        } catch (Exception e) {
            log.error("Failed to handle expired key: {}", expiredKey, e);
        }
    }

    @EventListener
    public void handleRedisKeyExpiredEvent(RedisKeyExpiredEvent<byte[]> event) {
        String expiredKey = new String((byte[]) event.getSource());

        // 기존 주문 타임아웃 처리 로직...

        // ✅ 추가: 구매 입찰 매칭 타임아웃 처리
        if (expiredKey.startsWith("buying-bid:match-timeout:")) {
            String buyingBidId = expiredKey.replace("buying-bid:match-timeout:", "");

            log.warn("BuyingBid match timeout expired: {}", buyingBidId);

            try {
                // MATCHED → LIVE 복구
                tradeClient.resetMatchedBid(UUID.fromString(buyingBidId));

                log.info("BuyingBid {} reset to LIVE due to buyer timeout", buyingBidId);
            } catch (Exception e) {
                log.error("Failed to reset BuyingBid {}", buyingBidId, e);
            }
        }
    }

    // ✅ 배송 기한 만료 핸들러
    private void handleShipmentExpired(String expiredKey) {
        try {
            // Key 형식: order:shipment-deadline:{orderId}
            String[] parts = expiredKey.split(":");

            // 유효성 검증 (prefix:part1:part2 -> 총 3부분이어야 함)
            if (parts.length < 3) {
                log.warn("Invalid shipment expired key format: {}", expiredKey);
                return;
            }

            // ID 추출
            UUID orderId = UUID.fromString(parts[2]);

            log.info("Triggering Shipment Overdue Process for Order: {}", orderId);

            // 서비스 호출 -> DB 상태 변경(CANCEL) 및 Kafka 이벤트 발행
            orderService.processShipmentOverdue(orderId);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in shipment expired key: {}", expiredKey, e);
        } catch (Exception e) {
            log.error("Failed to handle shipment expired key: {}", expiredKey, e);
        }
    }
}
