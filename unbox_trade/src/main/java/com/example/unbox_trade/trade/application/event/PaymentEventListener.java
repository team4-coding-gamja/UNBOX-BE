package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_trade.trade.application.service.AdminSellingBidService;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final SellingBidService sellingBidService;

    /**
     * ✅ 결제 완료 이벤트 (Kafka)
     * Payment 서비스에서 결제가 완료되면 입찰 상태를 SOLD로 변경합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "trade-group")
    public void handlePaymentEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in ProductEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        if (event instanceof PaymentCompletedEvent paymentCompletedEvent) {
            log.info("Received PaymentCompletedEvent for Order ID: {}, SellingBid ID: {}",
                    paymentCompletedEvent.orderId(), paymentCompletedEvent.sellingBidId());

            try {
                // 결제 완료 -> 판매 확정 (RESERVED -> SOLD)
                sellingBidService.soldSellingBid(
                        paymentCompletedEvent.sellingBidId(),
                        "PAYMENT_EVENT"
                );
                log.info("Successfully marked SellingBid {} as SOLD.", paymentCompletedEvent.sellingBidId());
            } catch (Exception e) {
                log.error("Failed to mark SellingBid {} as SOLD.", paymentCompletedEvent.sellingBidId(), e);
                throw e;
            }
        } else {
            log.warn("Unknown event type: {} (Value: {})", event.getClass().getName(), event);
        }

        ack.acknowledge();
    }

}
