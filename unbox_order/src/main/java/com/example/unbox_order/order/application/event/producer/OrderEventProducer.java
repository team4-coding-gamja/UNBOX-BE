package com.example.unbox_order.order.application.event.producer;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_common.event.order.OrderConfirmedEvent;
import com.example.unbox_common.event.order.OrderExpiredEvent;
import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 토픽 이름은 상수로 관리
    private static final String TOPIC_ORDER = "order-events";

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent: orderId={}, sellingBidId={}", event.orderId(), event.sellingBidId());

        String key = event.sellingBidId() != null ? event.sellingBidId().toString()
                : event.buyingBidId() != null ? event.buyingBidId().toString() : event.orderId().toString();

        kafkaTemplate.send(TOPIC_ORDER, key, event);
    }

    public void publishOrderExpired(OrderExpiredEvent event) {
        log.info("Publishing OrderExpiredEvent: orderId={}, sellingBidId={}", event.orderId(), event.sellingBidId());

        String key = event.sellingBidId() != null ? event.sellingBidId().toString()
                : event.buyingBidId() != null ? event.buyingBidId().toString() : event.orderId().toString();

        kafkaTemplate.send(TOPIC_ORDER, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // Kafka 전송 실패: 브로커 장애 등이 원인이므로 DLT 전송도 불가능함.
                        // 따라서 ERROR 레벨 로그를 남겨 모니터링 시스템(Sentry, ELK 등)이 감지하도록 함.
                        log.error("Failed to publish OrderExpiredEvent for orderId: {}", event.orderId(), ex);
                    } else {
                        log.debug("Successfully published OrderExpiredEvent: {}", result.getRecordMetadata());
                    }
                });
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Publishing OrderConfirmedEvent: orderId={}, userId={}", event.orderId(), event.userId());
        // 구매 확정은 주문(Order) 라이프사이클의 종료이므로 OrderId를 키로 사용
        kafkaTemplate.send(TOPIC_ORDER, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderConfirmedEvent for orderId: {}", event.orderId(), ex);
                    }
                });
    }

    public void publishRefundRequested(OrderRefundRequestedEvent event) {
        log.info("Publishing OrderRefundRequestedEvent: orderId={}, sellingBidId={}, paymentId={}",
                event.orderId(), event.sellingBidId(), event.paymentId());

        // 키(Key) 생성: sellingBidId 우선 -> buyingBidId -> orderId
        String key = event.sellingBidId() != null ? event.sellingBidId().toString()
                : event.buyingBidId() != null ? event.buyingBidId().toString() : event.orderId().toString();

        kafkaTemplate.send(TOPIC_ORDER, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderRefundRequestedEvent for orderId: {}", event.orderId(), ex);
                    } else {
                        log.debug("Successfully published OrderRefundRequestedEvent: {}", result.getRecordMetadata());
                    }
                });
    }
}
