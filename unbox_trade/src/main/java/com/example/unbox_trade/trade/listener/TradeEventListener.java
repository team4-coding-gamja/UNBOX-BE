package com.example.unbox_trade.trade.listener;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventListener {

    private final SellingBidService sellingBidService;

    @KafkaListener(topics = "payment-events", groupId = "trade-group")
    @Transactional
    public void handlePaymentEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

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
                // 중요: 여기서 실패하면 입찰 상태 불일치(결제는 됐는데 SOLD가 아님) 발생.
                // 재시도(Retry)를 위해 예외를 던짐
                throw e;
            }
        } else {
            log.warn("Unknown event type: {} (Value: {})", event.getClass().getName(), event);
        }
        
        // 처리 완료 커밋
        ack.acknowledge();
    }
}
