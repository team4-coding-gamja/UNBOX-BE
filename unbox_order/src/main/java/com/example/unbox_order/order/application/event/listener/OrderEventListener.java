package com.example.unbox_order.order.application.event.listener;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_order.order.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderService orderService;

    /**
     * ✅ 결제 완료 이벤트 리스너
     * Payment 서비스에서 결제 완료 시 발행하는 이벤트(PaymentCompletedEvent)를 수신하여
     * 주문 상태를 PENDING_SHIPMENT로 변경합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "order-group")
    public void handlePaymentCompletedEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in OrderEventListener. Key: {}", record.key());
            ack.acknowledge(); // Null 이벤트는 처리한 것으로 간주
            return;
        }

        if (event instanceof PaymentCompletedEvent paymentCompletedEvent) {
            log.info("Received PaymentCompletedEvent for Order ID: {}, SellingBid ID: {}", 
                    paymentCompletedEvent.orderId(), paymentCompletedEvent.sellingBidId());
            
            try {
                // 주문 상태 변경 (PAYMENT_PENDING -> PENDING_SHIPMENT)
                orderService.pendingShipmentOrder(paymentCompletedEvent.orderId(), "EVENT_LISTENER");
                log.info("Successfully updated Order {} status to PENDING_SHIPMENT.", paymentCompletedEvent.orderId());
            } catch (Exception e) {
                log.error("Failed to update Order {} status for PaymentCompletedEvent.", paymentCompletedEvent.orderId(), e);
                // 예외를 던져서 Retry 매커니즘(DefaultErrorHandler)이 동작하도록 함
                throw e;
            }
        } else {
            log.debug("Ignored event type: {}", event.getClass().getName());
        }

        ack.acknowledge();
    }
}
