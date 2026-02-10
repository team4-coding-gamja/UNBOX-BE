package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.payment.PaymentFailedEvent;
import com.example.unbox_trade.trade.application.service.BuyingBidInternalService;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final SellingBidService sellingBidService;
    private final BuyingBidInternalService buyingBidInternalService;

    /**
     * ✅ 결제 완료/실패 이벤트 수신 (Kafka Consumer)
     * - PaymentCompletedEvent: 결제 성공 -> 입찰 SOLD 처리
     * - PaymentFailedEvent: 결제 실패 -> 입찰 LIVE 복구 처리
     */
    // @KafkaListener(topics = "payment-events", groupId = "trade-group")
    public void handlePaymentEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        // 1. 이벤트 유효성 검사 (Null Check)
        if (event == null) {
            log.warn("Received null event in PaymentEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        // 2. 결제 완료 이벤트 처리 (PaymentCompletedEvent)
        if (event instanceof PaymentCompletedEvent paymentCompletedEvent) {
            handlePaymentCompleted(paymentCompletedEvent);
        }
        // 3. 결제 실패 이벤트 처리 (PaymentFailedEvent)
        else if (event instanceof PaymentFailedEvent paymentFailedEvent) {
            handlePaymentFailed(paymentFailedEvent);
        } else {
            // 알 수 없는 이벤트 타입인 경우 경고 로그 출력
            log.warn("Unknown event type: {} (Value: {})", event.getClass().getName(), event);
        }

        // 4. 메시지 처리 완료 (Commit)
        ack.acknowledge();
    }

    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for Order ID: {}", event.orderId());
        try {
            // A. 판매 입찰(SellingBid) -> SOLD
            if (event.sellingBidId() != null) {
                sellingBidService.soldSellingBid(event.sellingBidId(), "PAYMENT_EVENT");
                log.info("Successfully marked SellingBid {} as SOLD.", event.sellingBidId());
            }

            // B. 구매 입찰(BuyingBid) -> SOLD
            if (event.buyingBidId() != null) {
                buyingBidInternalService.soldBuyingBid(event.buyingBidId(), "PAYMENT_EVENT");
                log.info("Successfully marked BuyingBid {} as SOLD.", event.buyingBidId());
            }
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent for Order ID: {}", event.orderId(), e);
        }
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for Order ID: {}, Error: {}", event.orderId(), event.errorMessage());
        try {
            // A. 판매 입찰(SellingBid) -> LIVE (복구)
            if (event.sellingBidId() != null) {
                sellingBidService.liveSellingBid(event.sellingBidId(), "PAYMENT_FAIL_REVERT");
                log.info("Successfully reverted SellingBid {} to LIVE.", event.sellingBidId());
            }

            // B. 구매 입찰(BuyingBid) -> LIVE (복구)
            if (event.buyingBidId() != null) {
                buyingBidInternalService.liveBuyingBid(event.buyingBidId(), "PAYMENT_FAIL_REVERT");
                log.info("Successfully reverted BuyingBid {} to LIVE.", event.buyingBidId());
            }
        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent for Order ID: {}", event.orderId(), e);
        }
    }
}
