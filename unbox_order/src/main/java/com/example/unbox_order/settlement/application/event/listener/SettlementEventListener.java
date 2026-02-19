package com.example.unbox_order.settlement.application.event.listener;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_order.settlement.application.service.SettlementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 정산 이벤트 리스너
 * 
 * EventEnvelope 표준 규격 사용:
 * - payment-events: 결제 완료 시 정산 생성
 * - order-events: 환불 요청 시 정산 취소
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 결제 완료 이벤트 리스너 (정산 생성)
     * Payment 서비스에서 결제 완료 시 발행하는 이벤트(PaymentCompletedEvent)를 수신하여
     * 정산(Settlement) 데이터를 생성합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "settlement-group")
    public void handlePaymentCompletedEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventJson = record.value();

        if (eventJson == null || eventJson.isEmpty()) {
            log.warn("[SettlementEventListener] Received null or empty event (payment-events). Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        try {
            // 1. EventEnvelope 파싱
            EventEnvelope envelope = parseEnvelope(eventJson);

            log.debug("[SettlementEventListener] Received payment event - eventId: {}, eventType: {}",
                    envelope.getEventId(), envelope.getEventType());

            // 2. eventType 분기
            if ("PaymentCompleted".equals(envelope.getEventType())) {
                // 3. data 필드에서 실제 이벤트 추출
                PaymentCompletedEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        PaymentCompletedEvent.class);

                log.info("[SettlementEventListener] Processing PaymentCompletedEvent - orderId: {}, paymentId: {}",
                        event.orderId(), event.paymentId());

                try {
                    // 4. 비동기로 정산 생성
                    settlementService.createSettlementForPayment(event.paymentId());

                    log.info("[SettlementEventListener] ✅ Successfully created settlement for PaymentId: {}",
                            event.paymentId());
                } catch (Exception e) {
                    log.error("[SettlementEventListener] ❌ Failed to create settlement for PaymentId: {}",
                            event.paymentId(), e);
                    // 재시도 대상 (일시적 DB 장애 등)
                    throw e;
                }
            } else {
                log.debug("[SettlementEventListener] Ignored payment event type: {}", envelope.getEventType());
            }
        } catch (Exception e) {
            log.error("[SettlementEventListener] Failed to parse payment event JSON: {}", eventJson, e);
            throw new RuntimeException("Event parsing failed", e);
        }

        ack.acknowledge();
    }

    /**
     * ✅ 주문 이벤트 리스너 (정산 취소)
     * - Refund Requested -> Settlement Cancel
     * - Shipment Expired -> Settlement Cancel (Penalty)
     */
    @KafkaListener(topics = "order-events", groupId = "settlement-group")
    public void handleOrderEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventJson = record.value();

        if (eventJson == null || eventJson.isEmpty()) {
            log.warn("[SettlementEventListener] Received null or empty event (order-events). Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        try {
            // 1. EventEnvelope 파싱
            EventEnvelope envelope = parseEnvelope(eventJson);

            log.debug("[SettlementEventListener] Received order event - eventId: {}, eventType: {}",
                    envelope.getEventId(), envelope.getEventType());

            // 2. eventType 분기
            if ("OrderRefundRequested".equals(envelope.getEventType())) {
                OrderRefundRequestedEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        OrderRefundRequestedEvent.class);

                log.info("[SettlementEventListener] Processing OrderRefundRequestedEvent - orderId: {}",
                        event.orderId());
                cancelSettlement(event.orderId());

            } else if ("OrderShipmentExpired".equals(envelope.getEventType())) {
                com.example.unbox_common.event.order.OrderShipmentExpiredEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        com.example.unbox_common.event.order.OrderShipmentExpiredEvent.class);

                log.info("[SettlementEventListener] Processing OrderShipmentExpiredEvent - orderId: {}",
                        event.orderId());
                cancelSettlement(event.orderId());

            } else {
                log.debug("[SettlementEventListener] Ignored order event type: {}", envelope.getEventType());
            }
        } catch (Exception e) {
            log.error("[SettlementEventListener] Failed to parse order event JSON: {}", eventJson, e);
            throw new RuntimeException("Event parsing failed", e);
        }

        ack.acknowledge();
    }

    /**
     * JSON 문자열이 한 번 더 인코딩된 payload("\"{...}\"")도 처리합니다.
     */
    private EventEnvelope parseEnvelope(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);
        if (root.isTextual()) {
            return objectMapper.readValue(root.asText(), EventEnvelope.class);
        }
        return objectMapper.treeToValue(root, EventEnvelope.class);
    }

    /**
     * 정산 취소 공통 로직
     */
    private void cancelSettlement(UUID orderId) {
        try {
            settlementService.cancelSettlementByOrderId(orderId);
            log.info("[SettlementEventListener] ✅ Successfully cancelled settlement for OrderId: {}", orderId);
        } catch (Exception e) {
            log.error("[SettlementEventListener] ❌ Failed to cancel settlement for OrderId: {}", orderId, e);
            throw e;
        }
    }
}
