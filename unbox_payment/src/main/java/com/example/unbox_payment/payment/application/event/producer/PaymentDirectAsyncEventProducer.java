package com.example.unbox_payment.payment.application.event.producer;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.payment.PaymentFailedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 테스트 전용 Direct Async 이벤트 발행자
 * 
 * ⚠️ 주의: 이 클래스는 오직 성능 테스트 및 데이터 유실 검증 목적으로만 사용됩니다.
 * 
 * 목적:
 * - Outbox 패턴 vs Direct Async 방식의 데이터 정합성 비교
 * - Kafka 장애 시 이벤트 유실 가능성 검증
 * 
 * 사용 방법:
 * - HTTP 헤더에 "X-Test-Mode: async" 추가 시에만 활성화
 * - 평소에는 PaymentOutboxWriter를 통한 Outbox 패턴 사용
 * 
 * 문제점:
 * - Kafka 브로커 장애 시 이벤트 유실 가능
 * - Payment는 저장되었으나 Trade/Order 서비스는 이벤트를 받지 못함
 * - 데이터 불일치 발생 (Consistency 보장 불가)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentDirectAsyncEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "payment-events";

    /**
     * PaymentCompletedEvent를 즉시 Kafka로 발행 (Fire-and-Forget)
     * 
     * ⚠️ 문제점:
     * - Kafka 전송 실패 시 재시도 없음
     * - Payment 트랜잭션과 분리되어 있어 원자성 보장 불가
     * - 브로커 장애 시 이벤트 유실
     */
    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            UUID aggregateId = determineAggregateId(event);
            String key = aggregateId.toString();
            String payload = toEnvelopeJson(event, "PaymentCompleted", aggregateId);

            log.info("[DirectAsync] PaymentCompleted 이벤트 발행 시도 - paymentId: {}, key: {}",
                    event.paymentId(), key);

            // Fire-and-Forget: 결과를 기다리지 않음 (실패 시 재시도 없음)
            kafkaTemplate.send(TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[DirectAsync] ⚠️ PaymentCompleted 이벤트 발행 실패 - paymentId: {}, error: {}",
                                    event.paymentId(), ex.getMessage());
                            // ❌ 실패해도 재시도 없음 - 데이터 유실!
                        } else {
                            log.info(
                                    "[DirectAsync] PaymentCompleted 이벤트 발행 성공 - paymentId: {}, partition: {}, offset: {}",
                                    event.paymentId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            log.error("[DirectAsync] ⚠️ PaymentCompleted 이벤트 발행 중 예외 발생 - paymentId: {}, error: {}",
                    event.paymentId(), e.getMessage(), e);
            // ❌ 예외 발생해도 Payment는 이미 저장됨 - 데이터 불일치!
        }
    }

    /**
     * PaymentFailedEvent를 즉시 Kafka로 발행 (Fire-and-Forget)
     */
    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            UUID aggregateId = determineAggregateId(event);
            String key = aggregateId.toString();
            String payload = toEnvelopeJson(event, "PaymentFailed", aggregateId);

            log.info("[DirectAsync] PaymentFailed 이벤트 발행 시도 - paymentId: {}, key: {}",
                    event.paymentId(), key);

            kafkaTemplate.send(TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[DirectAsync] ⚠️ PaymentFailed 이벤트 발행 실패 - paymentId: {}, error: {}",
                                    event.paymentId(), ex.getMessage());
                        } else {
                            log.info("[DirectAsync] PaymentFailed 이벤트 발행 성공 - paymentId: {}",
                                    event.paymentId());
                        }
                    });

        } catch (Exception e) {
            log.error("[DirectAsync] ⚠️ PaymentFailed 이벤트 발행 중 예외 발생 - paymentId: {}, error: {}",
                    event.paymentId(), e.getMessage(), e);
        }
    }

    /**
     * Async 경로에서도 Outbox와 동일한 EventEnvelope 형식으로 직렬화
     */
    private String toEnvelopeJson(Object data, String eventType, UUID aggregateId) {
        try {
            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .occurredAt(LocalDateTime.now())
                    .aggregateId(aggregateId)
                    .data(data)
                    .build();
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DirectAsync envelope serialization failed", e);
        }
    }

    /**
     * Kafka 파티션 키 결정 (순서 보장용)
     */
    private UUID determineAggregateId(PaymentCompletedEvent event) {
        if (event.sellingBidId() != null) {
            return event.sellingBidId();
        }
        if (event.buyingBidId() != null) {
            return event.buyingBidId();
        }
        return event.orderId();
    }

    private UUID determineAggregateId(PaymentFailedEvent event) {
        if (event.sellingBidId() != null) {
            return event.sellingBidId();
        }
        if (event.buyingBidId() != null) {
            return event.buyingBidId();
        }
        return event.orderId();
    }
}
