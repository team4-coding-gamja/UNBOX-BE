package com.example.unbox_payment.payment.application.service;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.payment.PaymentFailedEvent;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEventStatus;
import com.example.unbox_payment.payment.domain.repository.PaymentOutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 이벤트를 아웃박스 테이블에 저장하는 Writer
 * Kafka에 직접 발행하지 않고 DB의 아웃박스 테이블에 저장만 수행
 * 실제 Kafka 발행은 OutboxMessageRelay가 담당
 * 
 * EventEnvelope 표준 규격을 사용하여 모든 이벤트에 메타데이터 포함:
 * - eventId: 추적 가능한 고유 ID
 * - eventType: 컨슈머가 분류에 사용
 * - occurredAt: 이벤트 발생 시각
 * - aggregateId: 파티션 키 (순서 보장)
 * - data: 실제 비즈니스 데이터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxWriter {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final ObjectMapper objectMapper;

    private static final String AGGREGATE_TYPE = "PAYMENT";

    /**
     * PaymentCompletedEvent를 아웃박스에 저장
     * 트랜잭션 내에서 실행되어 Payment 저장과 함께 원자적으로 처리됨
     */
    @Transactional
    public void write(PaymentCompletedEvent event) {
        log.info(
                "[OutboxWriter] PaymentCompletedEvent 저장: paymentKey={}, orderId={}, sellingBidId={}, buyingBidId={}",
                event.paymentKey(), event.orderId(), event.sellingBidId(), event.buyingBidId());

        // Key ID 결정 (Trade 서비스의 입찰 상태 변경 순서 보장)
        UUID keyId = determineKeyId(event.sellingBidId(), event.buyingBidId(), event.orderId());

        // ✅ 1단계: 아웃박스 이벤트를 먼저 생성 (ID 자동 생성)
        PaymentOutboxEvent paymentOutboxEvent = PaymentOutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(keyId)
                .eventType("PaymentCompleted")
                .payload("") // 임시 빈 문자열
                .status(PaymentOutboxEventStatus.PENDING)
                .retryCount(0)
                .build();

        // ✅ 2단계: 저장하여 ID 생성
        paymentOutboxEvent = paymentOutboxEventRepository.save(paymentOutboxEvent);

        // ✅ 3단계: 생성된 ID로 EventEnvelope 생성
        String payload = toJsonWithEnvelope(event, paymentOutboxEvent.getId(), "PaymentCompleted", keyId);

        // ✅ 4단계: payload 업데이트
        paymentOutboxEvent.setPayload(payload);
        paymentOutboxEventRepository.save(paymentOutboxEvent);

        log.info("[OutboxWriter] 아웃박스에 저장 완료: eventId={}, eventType=PaymentCompleted", paymentOutboxEvent.getId());
    }

    /**
     * PaymentFailedEvent를 아웃박스에 저장
     */
    @Transactional
    public void write(PaymentFailedEvent event) {
        log.info("[OutboxWriter] PaymentFailedEvent 저장: paymentId={}, orderId={}, sellingBidId={}, buyingBidId={}",
                event.paymentId(), event.orderId(), event.sellingBidId(), event.buyingBidId());

        // Key ID 결정
        UUID keyId = determineKeyId(event.sellingBidId(), event.buyingBidId(), event.orderId());

        // ✅ 1단계: 아웃박스 이벤트를 먼저 생성 (ID 자동 생성)
        PaymentOutboxEvent paymentOutboxEvent = PaymentOutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(keyId)
                .eventType("PaymentFailed")
                .payload("") // 임시 빈 문자열
                .status(PaymentOutboxEventStatus.PENDING)
                .retryCount(0)
                .build();

        // ✅ 2단계: 저장하여 ID 생성
        paymentOutboxEvent = paymentOutboxEventRepository.save(paymentOutboxEvent);

        // ✅ 3단계: 생성된 ID로 EventEnvelope 생성
        String payload = toJsonWithEnvelope(event, paymentOutboxEvent.getId(), "PaymentFailed", keyId);

        // ✅ 4단계: payload 업데이트
        paymentOutboxEvent.setPayload(payload);
        paymentOutboxEventRepository.save(paymentOutboxEvent);

        log.info("[OutboxWriter] 아웃박스에 저장 완료: eventId={}, eventType=PaymentFailed", paymentOutboxEvent.getId());
    }

    /**
     * Kafka 파티션 키 결정 (순서 보장용)
     */
    private UUID determineKeyId(UUID sellingBidId, UUID buyingBidId, UUID orderId) {
        if (sellingBidId != null) {
            return sellingBidId;
        }
        if (buyingBidId != null) {
            return buyingBidId;
        }
        return orderId; // Fallback
    }

    /**
     * 객체를 EventEnvelope로 감싸서 JSON 문자열로 변환
     * 
     * Envelope Pattern을 사용하여 모든 이벤트에 메타데이터 포함:
     * - eventId: 아웃박스 이벤트 ID (추적용)
     * - eventType: 이벤트 타입 문자열
     * - occurredAt: 발생 시각
     * - aggregateId: 집합 루트 ID
     * - data: 실제 비즈니스 데이터
     */
    private String toJsonWithEnvelope(Object eventData, UUID eventId, String eventType, UUID aggregateId) {
        try {
            // EventEnvelope로 감싸기 (표준 규격)
            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .occurredAt(LocalDateTime.now())
                    .aggregateId(aggregateId)
                    .data(eventData)
                    .build();

            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("[OutboxWriter] JSON 변환 실패: eventType={}, eventId={}",
                    eventType, eventId, e);
            throw new RuntimeException("이벤트 JSON 변환 실패", e);
        }
    }
}
