package com.example.unbox_payment.payment.application.event.listener;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_common.event.order.OrderShipmentExpiredEvent;
import com.example.unbox_payment.payment.application.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 주문 환불 요청 이벤트 리스너
 * Order 서비스에서 발행한 OrderRefundRequestedEvent를 수신하여
 * 토스 결제 취소 API를 호출하고 Payment 상태를 CANCELED로 변경합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRefundEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void handleOrderEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventJson = record.value();

        if (eventJson == null || eventJson.isEmpty()) {
            log.warn("Received null or empty event in OrderRefundEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(eventJson, EventEnvelope.class);
        } catch (Exception e) {
            log.error("Failed to parse order event envelope: {}", eventJson, e);
            throw new RuntimeException("Order event envelope parsing failed", e);
        }

        try {
            if ("OrderRefundRequested".equals(envelope.getEventType())) {
                OrderRefundRequestedEvent refundEvent = objectMapper.convertValue(
                        envelope.getData(),
                        OrderRefundRequestedEvent.class);
                log.info("Received OrderRefundRequestedEvent - orderId: {}, paymentId: {}, amount: {}",
                        refundEvent.orderId(), refundEvent.paymentId(), refundEvent.refundAmount());
                processRefund(refundEvent.paymentId(), refundEvent.reason(), refundEvent.orderId());
            } else if ("OrderShipmentExpired".equals(envelope.getEventType())) {
                OrderShipmentExpiredEvent expiredEvent = objectMapper.convertValue(
                        envelope.getData(),
                        OrderShipmentExpiredEvent.class);
                log.info("Received OrderShipmentExpiredEvent - orderId: {}, paymentId: {}",
                        expiredEvent.orderId(), expiredEvent.paymentId());
                processRefund(expiredEvent.paymentId(), "Shipment Timeout", expiredEvent.orderId());
            } else {
                log.debug("Ignored event type in OrderRefundEventListener: {}", envelope.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process order event - eventType: {}", envelope.getEventType(), e);
            throw e;
        }

        ack.acknowledge();
    }

    private void processRefund(java.util.UUID paymentId, String reason, java.util.UUID orderId) {
        try {
            paymentService.processRefund(paymentId, reason);
            log.info("Successfully processed refund for orderId: {}, paymentId: {}", orderId, paymentId);
        } catch (Exception e) {
            log.error("Failed to process refund for orderId: {}, paymentId: {}, error: {}",
                    orderId, paymentId, e.getMessage(), e);
            throw e;
        }
    }
}
