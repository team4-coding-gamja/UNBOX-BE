package com.example.unbox_payment.payment.application.event.producer;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_PAYMENT = "payment-events";

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Publishing PaymentCompletedEvent: paymentKey={}, orderId={}, sellingBidId={}", 
                event.paymentKey(), event.orderId(), event.sellingBidId());
        
        // Key를 sellingBidId로 설정하여 Trade 서비스의 입찰 상태 변경 순서 보장
        kafkaTemplate.send(TOPIC_PAYMENT, event.sellingBidId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("이벤트 발행 실패: {}", event.sellingBidId(), ex);
                    } else {
                        log.debug("이벤트 발행 성공: {}", result.getRecordMetadata());
                    }
                });
    }
}
