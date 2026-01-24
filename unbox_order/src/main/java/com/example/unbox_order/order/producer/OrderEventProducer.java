package com.example.unbox_order.order.producer;

import com.example.unbox_common.event.order.OrderCancelledEvent;
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
        // 키(Key)를 지정하여 해당 입찰(sellingBid) 관련 이벤트가 항상 동일 파티션으로 가도록 보장
        kafkaTemplate.send(TOPIC_ORDER, event.sellingBidId().toString(), event);
    }
}
