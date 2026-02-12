package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.payment.PaymentFailedEvent;
import com.example.unbox_trade.trade.application.service.BuyingBidInternalService;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 이벤트 리스너 (거래 서비스)
 * 
 * EventEnvelope 표준 규격 사용:
 * - PaymentCompletedEvent: 결제 성공 -> 입찰 SOLD 처리
 * - PaymentFailedEvent: 결제 실패 -> 입찰 LIVE 복구 처리
 * 
 * 장점:
 * 1. 단일 토픽(payment-events)에서 여러 이벤트 타입 처리 가능
 * 2. 서비스 간 결합도 감소 (클래스 타입 의존성 제거)
 * 3. 알 수 없는 이벤트는 안전하게 무시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final SellingBidService sellingBidService;
    private final BuyingBidInternalService buyingBidInternalService;
    private final ObjectMapper objectMapper;

    /**
     * ✅ 결제 완료/실패 이벤트 수신 (Kafka Consumer)
     */
    @KafkaListener(topics = "payment-events", groupId = "trade-group")
    public void handlePaymentEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String eventJson = record.value();

        // 1. 이벤트 유효성 검사
        if (eventJson == null || eventJson.isEmpty()) {
            log.warn("[PaymentEventListener] Received null or empty event. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        EventEnvelope envelope = null;
        try {
            // 2. EventEnvelope 파싱
            envelope = objectMapper.readValue(eventJson, EventEnvelope.class);

            log.debug("[PaymentEventListener] Received event - eventId: {}, eventType: {}, aggregateId: {}",
                    envelope.getEventId(), envelope.getEventType(), envelope.getAggregateId());

        } catch (Exception e) {
            // 파싱 실패 (JSON 형식 오류 등)
            log.error("[PaymentEventListener] Failed to parse EventEnvelope JSON: {}", eventJson, e);
            throw new RuntimeException("Event parsing failed", e);
        }

        try {
            // 3. eventType에 따라 분기
            if ("PaymentCompleted".equals(envelope.getEventType())) {
                // data 필드에서 실제 이벤트 추출
                PaymentCompletedEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        PaymentCompletedEvent.class);
                handlePaymentCompleted(event);

            } else if ("PaymentFailed".equals(envelope.getEventType())) {
                // data 필드에서 실제 이벤트 추출
                PaymentFailedEvent event = objectMapper.convertValue(
                        envelope.getData(),
                        PaymentFailedEvent.class);
                handlePaymentFailed(event);

            } else {
                // 알 수 없는 이벤트 타입은 로그만 남기고 무시
                log.debug("[PaymentEventListener] Ignored event type: {}", envelope.getEventType());
            }
        } catch (Exception e) {
            // 비즈니스 로직 실패 (입찰 상태 변경 실패 등)
            log.error("[PaymentEventListener] Failed to process event - eventId: {}, eventType: {}",
                    envelope.getEventId(), envelope.getEventType(), e);
            throw e; // 재시도를 위해 예외 전파
        }

        // 4. 메시지 처리 완료 (Commit)
        ack.acknowledge();
    }

    /**
     * 결제 완료 이벤트 처리
     * - SellingBid -> SOLD
     * - BuyingBid -> SOLD
     */
    private void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[PaymentEventListener] Processing PaymentCompletedEvent - orderId: {}", event.orderId());

        try {
            // A. 판매 입찰(SellingBid) -> SOLD
            if (event.sellingBidId() != null) {
                sellingBidService.soldSellingBid(event.sellingBidId(), "PAYMENT_EVENT");
                log.info("[PaymentEventListener] ✅ Successfully marked SellingBid {} as SOLD",
                        event.sellingBidId());
            }

            // B. 구매 입찰(BuyingBid) -> SOLD
            if (event.buyingBidId() != null) {
                buyingBidInternalService.soldBuyingBid(event.buyingBidId(), "PAYMENT_EVENT");
                log.info("[PaymentEventListener] ✅ Successfully marked BuyingBid {} as SOLD",
                        event.buyingBidId());
            }
        } catch (Exception e) {
            log.error("[PaymentEventListener] ❌ Failed to process PaymentCompletedEvent - orderId: {}",
                    event.orderId(), e);
            // 재시도를 위해 예외를 다시 던짐
            throw e;
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * - SellingBid -> LIVE (복구)
     * - BuyingBid -> LIVE (복구)
     */
    private void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("[PaymentEventListener] Processing PaymentFailedEvent - orderId: {}, error: {}",
                event.orderId(), event.errorMessage());

        try {
            // A. 판매 입찰(SellingBid) -> LIVE (복구)
            if (event.sellingBidId() != null) {
                sellingBidService.liveSellingBid(event.sellingBidId(), "PAYMENT_FAIL_REVERT");
                log.info("[PaymentEventListener] ✅ Successfully reverted SellingBid {} to LIVE",
                        event.sellingBidId());
            }

            // B. 구매 입찰(BuyingBid) -> LIVE (복구)
            if (event.buyingBidId() != null) {
                buyingBidInternalService.liveBuyingBid(event.buyingBidId(), "PAYMENT_FAIL_REVERT");
                log.info("[PaymentEventListener] ✅ Successfully reverted BuyingBid {} to LIVE",
                        event.buyingBidId());
            }
        } catch (Exception e) {
            log.error("[PaymentEventListener] ❌ Failed to process PaymentFailedEvent - orderId: {}",
                    event.orderId(), e);
            // 재시도를 위해 예외를 다시 던짐
            throw e;
        }
    }
}
