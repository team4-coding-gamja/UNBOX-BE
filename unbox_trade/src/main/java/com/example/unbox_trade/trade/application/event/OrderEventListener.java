package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_common.event.order.OrderExpiredEvent;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final SellingBidRepository sellingBidRepository;
    private final SellingBidService sellingBidService;

    @KafkaListener(topics = "order-events", groupId = "trade-group")
    @Transactional
    public void handleOrderEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();
        
        if (event instanceof OrderCancelledEvent cancelledEvent) {
            log.info("Received OrderCancelledEvent for Order ID: {}, SellingBid ID: {}", cancelledEvent.orderId(), cancelledEvent.sellingBidId());
            revertSellingBid(cancelledEvent.sellingBidId(), ack);
        } else if (event instanceof OrderExpiredEvent expiredEvent) {
            log.info("Received OrderExpiredEvent for Order ID: {}, SellingBid ID: {}", expiredEvent.orderId(), expiredEvent.sellingBidId());
            revertSellingBid(expiredEvent.sellingBidId(), ack);
        } else {
            log.warn("Unknown event type: {} (Value: {})", event.getClass().getName(), event);
            ack.acknowledge();
        }
    }

    private void revertSellingBid(UUID sellingBidId, Acknowledgment ack) {
        // 1. 조회 및 스마트 원복 검증 (Repository 사용)
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(sellingBidId)
                .orElse(null);

        if (sellingBid == null) {
            log.warn("SellingBid not found for revert: {}", sellingBidId);
            ack.acknowledge();
            return;
        }

        // 상태가 RESERVED가 아니면 건너뜀
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            log.info("Skipping revert: SellingBid {} status is {}, not RESERVED.", sellingBid.getId(), sellingBid.getStatus());
            ack.acknowledge();
            return;
        }

        // 만료일 지났으면 CANCELLED 처리
        if (sellingBid.getDeadline() != null && sellingBid.getDeadline().isBefore(LocalDateTime.now())) {
            log.info("Skipping revert: SellingBid {} has expired (deadline: {}). Setting status to CANCELLED.", sellingBid.getId(), sellingBid.getDeadline());
            sellingBid.updateStatus(SellingStatus.CANCELLED);
            ack.acknowledge();
            return;
        }
        
        // 2. 상태 원복 및 캐시 갱신 (Service 위임)
        // Service 내부에서 캐시 무효화(evict) 및 가격 변동 이벤트 발행을 수행함
        try {
            sellingBidService.liveSellingBid(sellingBidId, "SYSTEM_EVENT");
            log.info("Successfully reverted SellingBid {} status to LIVE via Service.", sellingBidId);
        } catch (Exception e) {
            log.error("Failed to revert SellingBid {} via Service.", sellingBidId, e);
            // 여기서 예외를 던지면 Kafka 재시도(Retry)가 동작함. 
            // 단, 이미 위에서 검증했으므로 비즈니스 로직 오류 가능성은 낮음.
            throw e; 
        }

        ack.acknowledge();
    }
}
