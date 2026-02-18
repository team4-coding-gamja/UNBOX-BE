package com.example.unbox_payment.payment.application.event.producer;

import com.example.unbox_payment.payment.application.service.PaymentOutboxService;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Outbox 이벤트를 Kafka로 발행하는 Producer
 * 
 * Self-Invocation 문제 해결 완료:
 * - PaymentOutboxService를 별도 빈으로 분리하여 프록시가 정상 작동
 * - Kafka 전송(네트워크 I/O)과 DB 업데이트(로컬 I/O)를 분리하여 성능 최적화
 * 
 * 작동 방식:
 * 1. Kafka로 JSON 문자열 전송 (트랜잭션 밖에서 실행)
 * 2. 전송 성공 시 PaymentOutboxService.markAsPublished() 호출 (별도 트랜잭션)
 * 3. 전송 실패 시 예외를 던져서 상위 레이어가 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentOutboxService outboxService; // 별도 서비스로 분리 (Self-Invocation 해결!)

    // Aggregate Type 기반 단일 토픽 (Payment 도메인)
    private static final String TOPIC_PAYMENT_EVENTS = "payment-events";

    /**
     * 아웃박스 이벤트를 Kafka로 발행
     * 
     * JSON 문자열을 직접 전송하여 서비스 간 결합도 최소화
     * EventEnvelope 표준 규격 사용으로 컨슈머의 이벤트 분류 용이
     * 
     * @param paymentOutboxEvent 발행할 아웃박스 이벤트
     */
    public void publishEvent(PaymentOutboxEvent paymentOutboxEvent) {
        log.debug("[OutboxRelay] 이벤트 발행 시도 - eventId: {}, eventType: {}, aggregateId: {}",
                paymentOutboxEvent.getId(),
                paymentOutboxEvent.getEventType(),
                paymentOutboxEvent.getAggregateId());

        try {
            // 1. 토픽 결정 (추상화된 메서드)
            String topic = resolveTopic(paymentOutboxEvent);

            // 2. 파티션 키 (순서 보장)
            String key = paymentOutboxEvent.getAggregateId().toString();

            // 3. JSON 문자열 (EventEnvelope 형식)
            String value = paymentOutboxEvent.getPayload();

            // 4. Kafka로 JSON 문자열 직접 전송 (Object 역직렬화 불필요!)
            var result = kafkaTemplate.send(topic, key, value)
                    .get(); // 브로커 ACK 수신까지 블로킹

            // 5. 발행 성공 처리 (외부 서비스 호출로 프록시 정상 작동!)
            outboxService.markAsPublished(paymentOutboxEvent.getId());

            log.info("[OutboxRelay] 이벤트 발행 성공 - eventId: {}, eventType: {}, topic: {}, partition: {}, offset: {}",
                    paymentOutboxEvent.getId(),
                    paymentOutboxEvent.getEventType(),
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            // Kafka 발행 실패 시 예외 발생 (상위에서 handlePublishFailure 호출됨)
            log.error("[OutboxRelay] Kafka 발행 실패 - eventId: {}, eventType: {}",
                    paymentOutboxEvent.getId(),
                    paymentOutboxEvent.getEventType(),
                    e);
            throw new RuntimeException("Kafka 발행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 토픽 결정 로직
     * 
     * 현재는 단일 토픽 사용 (payment-events)
     * 필요시 eventType에 따라 다른 토픽으로 라우팅 가능
     */
    private String resolveTopic(PaymentOutboxEvent event) {
        // Aggregate Type 기반: Payment 도메인의 모든 이벤트는 단일 토픽
        return TOPIC_PAYMENT_EVENTS;
    }
}
