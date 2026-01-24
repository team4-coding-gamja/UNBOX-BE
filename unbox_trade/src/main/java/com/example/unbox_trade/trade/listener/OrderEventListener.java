package com.example.unbox_trade.trade.listener;

import com.example.unbox_common.event.order.OrderCancelledEvent;
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
    public void handleOrderCancelled(OrderCancelledEvent event, Acknowledgment ack) {
        log.info("Received OrderCancelledEvent for Order ID: {}, SellingBid ID: {}", event.orderId(), event.sellingBidId());

        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(event.sellingBidId())
                .orElse(null);

        if (sellingBid == null) {
            log.warn("SellingBid not found for revert: {}", event.sellingBidId());
            ack.acknowledge(); // 비즈니스적으로 무시할 상황도 처리 완료로 간주
            return;
        }

        // 스마트 원복 로직 (Smart Revert Strategy)
        // 1. 상태가 RESERVED인지 확인 (이미 SOLD거나 CANCELLED면 건드리지 않음)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            log.info("Skipping revert: SellingBid {} status is {}, not RESERVED.", sellingBid.getId(), sellingBid.getStatus());
            ack.acknowledge();
            return;
        }

        // 2. 만료일 확인 (이미 만료된 입찰은 되살리지 않음)
        if (sellingBid.getDeadline() != null && sellingBid.getDeadline().isBefore(LocalDateTime.now())) {
            log.info("Skipping revert: SellingBid {} has expired (deadline: {}). Setting status to CANCELLED.", sellingBid.getId(), sellingBid.getDeadline());
            sellingBid.updateStatus(SellingStatus.CANCELLED);
            ack.acknowledge();
            return;
        }

        // 3. 상태 원복 (RESERVED -> LIVE)
        sellingBid.updateStatus(SellingStatus.LIVE);
        log.info("Successfully reverted SellingBid {} status to LIVE.", sellingBid.getId());
        
        // 수동 커밋 (처리가 완벽히 끝난 후)
        ack.acknowledge();
    }
}
