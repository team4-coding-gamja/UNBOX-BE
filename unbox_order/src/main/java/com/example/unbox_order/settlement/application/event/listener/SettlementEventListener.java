package com.example.unbox_order.settlement.application.event.listener;

import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;

import com.example.unbox_order.settlement.application.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    /**
     * ✅ 결제 완료 이벤트 리스너 (정산 생성)
     * Payment 서비스에서 결제 완료 시 발행하는 이벤트(PaymentCompletedEvent)를 수신하여
     * 정산(Settlement) 데이터를 생성합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "settlement-group")
    public void handlePaymentCompletedEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in SettlementEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        if (event instanceof PaymentCompletedEvent paymentCompletedEvent) {
            log.info("Received PaymentCompletedEvent for Settlement Creation. Order ID: {}, Payment Key: {}", 
                    paymentCompletedEvent.orderId(), paymentCompletedEvent.paymentKey());
            
            try {
                // 비동기로 정산 생성
                settlementService.createSettlementForPayment(paymentCompletedEvent.paymentId());
                log.info("Successfully created settlement via event for PaymentId: {}", paymentCompletedEvent.paymentId());
                
            } catch (Exception e) {
                log.error("Failed to create settlement for PaymentCompletedEvent. OrderId: {}", paymentCompletedEvent.orderId(), e);
                // 재시도 대상 (일시적 DB 장애 등)
                throw e; 
            }
        } else {
            log.debug("Ignored event type: {}", event.getClass().getName());
        }

        ack.acknowledge();
    }

    /**
     * ✅ 주문 환불 요청 이벤트 리스너 (정산 취소)
     * Order 서비스에서 환불 요청 시 발행하는 이벤트(OrderRefundRequestedEvent)를 수신하여
     * 정산(Settlement) 상태를 CANCELLED로 변경합니다.
     */
    @KafkaListener(topics = "order-events", groupId = "settlement-group")
    public void handleOrderRefundEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in SettlementEventListener (order-events). Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        if (event instanceof OrderRefundRequestedEvent refundEvent) {
            log.info("Received OrderRefundRequestedEvent for Settlement Cancellation. Order ID: {}", 
                    refundEvent.orderId());
            
            try {
                settlementService.cancelSettlementByOrderId(refundEvent.orderId());
                log.info("Successfully cancelled settlement for OrderId: {}", refundEvent.orderId());
                
            } catch (Exception e) {
                log.error("Failed to cancel settlement for OrderRefundRequestedEvent. OrderId: {}", 
                        refundEvent.orderId(), e);
                throw e;
            }
        } else {
            log.debug("Ignored event type in order-events: {}", event.getClass().getName());
        }

        ack.acknowledge();
    }
}

