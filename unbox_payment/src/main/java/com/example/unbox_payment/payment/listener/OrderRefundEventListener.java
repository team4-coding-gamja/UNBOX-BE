package com.example.unbox_payment.payment.listener;

import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_payment.payment.service.PaymentService;
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

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void handleOrderRefundRequested(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in OrderRefundEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        if (event instanceof OrderRefundRequestedEvent refundEvent) {
            log.info("Received OrderRefundRequestedEvent - orderId: {}, paymentId: {}, amount: {}",
                    refundEvent.orderId(), refundEvent.paymentId(), refundEvent.refundAmount());

            try {
                // 환불 처리 (토스 API 호출 + DB 상태 변경)
                paymentService.processRefund(refundEvent.paymentId(), refundEvent.reason());
                log.info("Successfully processed refund for orderId: {}, paymentId: {}",
                        refundEvent.orderId(), refundEvent.paymentId());
            } catch (Exception e) {
                log.error("Failed to process refund for orderId: {}, paymentId: {}, error: {}",
                        refundEvent.orderId(), refundEvent.paymentId(), e.getMessage(), e);
                // 예외를 던져서 Retry 매커니즘이 동작하도록 함
                throw e;
            }
        } else {
            log.debug("Ignored event type in OrderRefundEventListener: {}", event.getClass().getName());
        }

        ack.acknowledge();
    }
}
