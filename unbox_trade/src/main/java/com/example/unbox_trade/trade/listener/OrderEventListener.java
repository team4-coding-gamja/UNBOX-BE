package com.example.unbox_trade.trade.listener;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_common.event.order.OrderExpiredEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final SellingBidRepository sellingBidRepository;

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

    private void revertSellingBid(java.util.UUID sellingBidId, Acknowledgment ack) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(sellingBidId)
                .orElse(null);

        if (sellingBid == null) {
            log.warn("SellingBid not found for revert: {}", sellingBidId);
            ack.acknowledge();
            return;
        }

        // 스마트 원복 로직 (Smart Revert Strategy)
        // 1. 상태가 RESERVED인지 확인
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            log.info("Skipping revert: SellingBid {} status is {}, not RESERVED.", sellingBid.getId(), sellingBid.getStatus());
            ack.acknowledge();
            return;
        }

        // 2. 만료일 확인
        if (sellingBid.getDeadline() != null && sellingBid.getDeadline().isBefore(LocalDateTime.now())) {
            log.info("Skipping revert: SellingBid {} has expired (deadline: {}). Setting status to CANCELLED.", sellingBid.getId(), sellingBid.getDeadline());
            sellingBid.updateStatus(SellingStatus.CANCELLED);
            ack.acknowledge();
            return;
        }

        // 3. 상태 원복 (RESERVED -> LIVE)
        sellingBid.updateStatus(SellingStatus.LIVE);
        log.info("Successfully reverted SellingBid {} status to LIVE.", sellingBid.getId());

        ack.acknowledge();
    }
}
