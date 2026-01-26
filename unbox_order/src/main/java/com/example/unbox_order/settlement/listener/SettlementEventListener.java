package com.example.unbox_order.settlement.listener;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;

import com.example.unbox_order.settlement.service.SettlementService;
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
}
