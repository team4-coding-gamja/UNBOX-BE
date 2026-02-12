package com.example.unbox_order.order.application.event.listener;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_order.order.application.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트 리스너 (주문 서비스)
 * 
 * EventEnvelope 표준 규격 사용:
 * - StringDeserializer로 안전하게 수신
 * - eventType 필드로 이벤트 분류
 * - data 필드에서 실제 비즈니스 데이터 추출
 * 
 * 장점:
 * 1. 프로듀서의 클래스 타입에 의존하지 않음 (결합도 감소)
 * 2. 알 수 없는 이벤트 타입은 안전하게 무시
 * 3. 디버깅 용이 (JSON 문자열을 로그에서 직접 확인 가능)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 결제 완료 이벤트 리스너
     * Payment 서비스에서 결제 완료 시 발행하는 이벤트(PaymentCompletedEvent)를 수신하여
     * 주문 상태를 PENDING_SHIPMENT로 변경합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void handlePaymentCompletedEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventJson = record.value();

        if (eventJson == null || eventJson.isEmpty()) {
            log.warn("[OrderEventListener] Received null or empty event. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        try {
            // 1. EventEnvelope 파싱
            EventEnvelope envelope = objectMapper.readValue(eventJson, EventEnvelope.class);

            log.debug("[OrderEventListener] Received event - eventId: {}, eventType: {}, aggregateId: {}",
                    envelope.getEventId(), envelope.getEventType(), envelope.getAggregateId());

            // 2. eventType에 따라 분기
            if ("PaymentCompleted".equals(envelope.getEventType())) {
                // 3. data 필드에서 실제 이벤트 추출
                PaymentCompletedEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        PaymentCompletedEvent.class);

                log.info(
                        "[OrderEventListener] Processing PaymentCompletedEvent - orderId: {}, paymentId: {}, sellingBidId: {}",
                        event.orderId(), event.paymentId(), event.sellingBidId());

                try {
                    // 4. 비즈니스 로직 실행
                    orderService.pendingShipmentOrder(event.orderId(), event.paymentId(), "EVENT_LISTENER");

                    log.info("[OrderEventListener] ✅ Successfully updated Order {} to PENDING_SHIPMENT",
                            event.orderId());
                } catch (Exception e) {
                    log.error("[OrderEventListener] ❌ Failed to update Order {} status", event.orderId(), e);
                    // 예외를 던져서 Retry 매커니즘(DefaultErrorHandler)이 동작하도록 함
                    throw e;
                }
            } else {
                // 알 수 없는 이벤트 타입은 로그만 남기고 안전하게 무시
                log.debug("[OrderEventListener] Ignored event type: {}", envelope.getEventType());
            }
        } catch (Exception e) {
            log.error("[OrderEventListener] Failed to parse event JSON: {}", eventJson, e);
            throw new RuntimeException("Event parsing failed", e);
        }

        ack.acknowledge();
    }
}
