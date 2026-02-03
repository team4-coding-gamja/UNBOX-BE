package com.example.unbox_payment.payment.application.event.producer;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.payment.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_PAYMENT = "payment-events";

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent: paymentKey={}, orderId={}, sellingBidId={}, buyingBidId={}",
                event.paymentKey(), event.orderId(), event.sellingBidId(), event.buyingBidId());

        // Key를 sellingBidId 또는 buyingBidId로 설정하여 Trade 서비스의 입찰 상태 변경 순서 보장 (Order 서비스와
        // 동일 기준)
        UUID keyId = (event.sellingBidId() != null) ? event.sellingBidId() : event.buyingBidId();
        if (keyId == null) {
            keyId = event.orderId(); // Fallback
        }

        kafkaTemplate.send(TOPIC_PAYMENT, keyId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentCompletedEvent for orderId: {}", event.orderId(), ex);
                    } else {
                        log.debug("Successfully published PaymentCompletedEvent: {}", result.getRecordMetadata());
                    }
                });
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        log.info("Publishing PaymentFailedEvent: paymentId={}, orderId={}, sellingBidId={}, buyingBidId={}",
                event.paymentId(), event.orderId(), event.sellingBidId(), event.buyingBidId());

        // Key를 sellingBidId 또는 buyingBidId로 설정
        UUID keyId = (event.sellingBidId() != null) ? event.sellingBidId() : event.buyingBidId();
        if (keyId == null) {
            keyId = event.orderId(); // Fallback
        }

        kafkaTemplate.send(TOPIC_PAYMENT, keyId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentFailedEvent for orderId: {}", event.orderId(), ex);
                    } else {
                        log.debug("Successfully published PaymentFailedEvent: {}", result.getRecordMetadata());
                    }
                });
    }
}
