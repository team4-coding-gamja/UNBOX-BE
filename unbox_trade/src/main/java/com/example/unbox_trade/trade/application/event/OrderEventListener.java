package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_common.event.order.OrderExpiredEvent;
import com.example.unbox_common.event.order.OrderRefundRequestedEvent;
import com.example.unbox_common.event.order.OrderShipmentExpiredEvent;
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
    private final com.example.unbox_trade.trade.domain.repository.BuyingBidRepository buyingBidRepository;
    private final com.example.unbox_trade.trade.application.service.BuyingBidInternalService buyingBidInternalService;

    @KafkaListener(topics = "order-events", groupId = "trade-group")
    @Transactional
    public void handleOrderEvent(org.apache.kafka.clients.consumer.ConsumerRecord<String, Object> record,
            Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in OrderEventListener. Key: {}", record.key());
            ack.acknowledge();
            return;
        }

        if (event instanceof OrderCancelledEvent cancelledEvent) {
            log.info("Received OrderCancelledEvent for Order ID: {}", cancelledEvent.orderId());
            if (cancelledEvent.sellingBidId() != null) {
                revertSellingBid(cancelledEvent.sellingBidId(), ack);
            } else if (cancelledEvent.buyingBidId() != null) {
                revertBuyingBid(cancelledEvent.buyingBidId(), ack);
            } else {
                ack.acknowledge();
            }
        } else if (event instanceof OrderExpiredEvent expiredEvent) {
            log.info("Received OrderExpiredEvent for Order ID: {}", expiredEvent.orderId());
            if (expiredEvent.sellingBidId() != null) {
                revertSellingBid(expiredEvent.sellingBidId(), ack);
            } else if (expiredEvent.buyingBidId() != null) {
                revertBuyingBid(expiredEvent.buyingBidId(), ack);
            } else {
                ack.acknowledge();
            }
        } else if (event instanceof OrderRefundRequestedEvent refundEvent) {
            log.info("Received OrderRefundRequestedEvent for Order ID: {}, PreviousStatus: {}",
                    refundEvent.orderId(), refundEvent.previousStatus());
            if (refundEvent.sellingBidId() != null) {
                handleSellingRefund(refundEvent.sellingBidId(), refundEvent.previousStatus(), ack);
            } else if (refundEvent.buyingBidId() != null) {
                handleBuyingRefund(refundEvent.buyingBidId(), refundEvent.previousStatus(), ack);
            } else {
                ack.acknowledge();
            }
        } else if (event instanceof OrderShipmentExpiredEvent expiredEvent) {
            log.info("Received OrderShipmentExpiredEvent for Order ID: {}", expiredEvent.orderId());
            if (expiredEvent.sellingBidId() != null) {
                handleShipmentExpired(expiredEvent.sellingBidId(), ack);
            } else {
                ack.acknowledge();
            }
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
            log.info("Skipping revert: SellingBid {} status is {}, not RESERVED.", sellingBid.getId(),
                    sellingBid.getStatus());
            ack.acknowledge();
            return;
        }

        // 만료일 지났으면 CANCELLED 처리 (Service 위임하여 캐시 무효화 포함)
        if (sellingBid.getDeadline() != null && sellingBid.getDeadline().isBefore(LocalDateTime.now())) {
            log.info("SellingBid {} has expired (deadline: {}). Expiring via service.", sellingBid.getId(),
                    sellingBid.getDeadline());
            sellingBidService.expireSellingBid(sellingBidId);
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

    /**
     * 배송 기한 만료 처리
     * - SellingStatus: RESERVED/SOLD -> CANCELLED
     * - 판매자가 기한 내 발송하지 않음 -> 패널티성 취소
     */
    private void handleShipmentExpired(UUID sellingBidId, Acknowledgment ack) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(sellingBidId)
                .orElse(null);

        if (sellingBid == null) {
            log.warn("SellingBid not found for shipment expiration: {}", sellingBidId);
            ack.acknowledge();
            return;
        }

        // 이미 취소되었거나 완료된 상태 체크 (멱등성)
        if (sellingBid.getStatus() == SellingStatus.CANCELLED) {
            log.info("SellingBid {} is already CANCELLED. Skipping shipment expiration.", sellingBidId);
            ack.acknowledge();
            return;
        }

        try {
            // 시스템에 의한 강제 취소 (패널티)
            // -1L: System Admin
            sellingBidService.cancelSellingBid(sellingBidId, -1L, "SHIPMENT_EXPIRATION");
            log.info("SellingBid {} cancelled due to shipment expiration.", sellingBidId);
        } catch (Exception e) {
            log.error("Failed to process shipment expiration for SellingBid {}.", sellingBidId, e);
            throw e;
        }

        ack.acknowledge();
    }

    /**
     * 환불 요청 이벤트 처리
     * - PENDING_SHIPMENT (배송 전 취소): 입찰을 SOLD → LIVE로 복구 (판매자가 다시 올릴 필요 없음)
     * - DELIVERED (배송 후 반품): 입찰을 SOLD → CANCELLED로 변경 (반품된 상품)
     */
    private void handleSellingRefund(UUID sellingBidId, String previousStatus, Acknowledgment ack) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNullForUpdate(sellingBidId)
                .orElse(null);

        if (sellingBid == null) {
            log.warn("SellingBid not found for refund: {}", sellingBidId);
            ack.acknowledge();
            return;
        }

        // SOLD 상태가 아니면 이미 처리되었거나 잘못된 이벤트 (멱등성)
        if (sellingBid.getStatus() != SellingStatus.SOLD) {
            log.info("Skipping refund: SellingBid {} status is {}, not SOLD.", sellingBid.getId(),
                    sellingBid.getStatus());
            ack.acknowledge();
            return;
        }

        try {
            if ("PENDING_SHIPMENT".equals(previousStatus)) {
                // 배송 전 취소: LIVE로 복구 (판매자가 다시 올릴 필요 없음)
                sellingBid.updateStatus(SellingStatus.LIVE);
                log.info("SellingBid {} reverted to LIVE (pre-shipment refund).", sellingBidId);
            } else {
                // 배송 후 취소 (DELIVERED 등): CANCELLED로 변경
                sellingBid.updateStatus(SellingStatus.CANCELLED);
                sellingBid.softDelete("REFUND_EVENT");
                log.info("SellingBid {} cancelled (post-delivery refund).", sellingBidId);
            }
        } catch (Exception e) {
            log.error("Failed to process refund for SellingBid {}.", sellingBidId, e);
            throw e;
        }

        ack.acknowledge();
    }

    // ==========================================
    // Buying Bid Handlers
    // ==========================================

    private void revertBuyingBid(UUID buyingBidId, Acknowledgment ack) {
        com.example.unbox_trade.trade.domain.entity.BuyingBid buyingBid = buyingBidRepository
                .findByIdAndDeletedAtIsNull(buyingBidId)
                .orElse(null);

        if (buyingBid == null) {
            log.warn("BuyingBid not found for revert: {}", buyingBidId);
            ack.acknowledge();
            return;
        }

        if (buyingBid.getStatus() != com.example.unbox_trade.trade.domain.entity.BuyingStatus.RESERVED) {
            log.info("Skipping revert: BuyingBid {} status is {}, not RESERVED.", buyingBidId, buyingBid.getStatus());
            ack.acknowledge();
            return;
        }

        try {
            buyingBidInternalService.liveBuyingBid(buyingBidId, "SYSTEM_EVENT");
            log.info("Successfully reverted BuyingBid {} status to LIVE via Service.", buyingBidId);
        } catch (Exception e) {
            log.error("Failed to revert BuyingBid {} via Service.", buyingBidId, e);
            throw e;
        }
        ack.acknowledge();
    }

    private void handleBuyingRefund(UUID buyingBidId, String previousStatus, Acknowledgment ack) {
        com.example.unbox_trade.trade.domain.entity.BuyingBid buyingBid = buyingBidRepository
                .findByIdAndDeletedAtIsNull(buyingBidId)
                .orElse(null);

        if (buyingBid == null) {
            log.warn("BuyingBid not found for refund: {}", buyingBidId);
            ack.acknowledge();
            return;
        }

        if (buyingBid.getStatus() != com.example.unbox_trade.trade.domain.entity.BuyingStatus.SOLD) {
            log.info("Skipping refund: BuyingBid {} status is {}, not SOLD.", buyingBidId, buyingBid.getStatus());
            ack.acknowledge();
            return;
        }

        try {
            if ("PENDING_SHIPMENT".equals(previousStatus)) {
                // 배송 전 취소: 구매자가 취소했으므로 다시 입찰(LIVE) 상태로 돌릴지, 아니면 그냥 취소할지 정책 결정.
                // 일반적인 '즉시 판매' 거래에서 구매자(Bidder)가 취소했다면 입찰은 유지(LIVE)되어야 함.
                // 판매자(Seller)가 취소했다면 입찰은 유지(LIVE)되어야 함.
                // 여기서는 LIVE 복구로 처리.
                buyingBidInternalService.liveBuyingBid(buyingBidId, "REFUND_EVENT");
                log.info("BuyingBid {} reverted to LIVE (pre-shipment refund).", buyingBidId);
            } else {
                // 배송 후 취소: 거래 종료(CANCELLED)
                buyingBidInternalService.expireBuyingBid(buyingBidId); // or explicit cancel
                log.info("BuyingBid {} cancelled (post-delivery refund).", buyingBidId);
            }
        } catch (Exception e) {
            log.error("Failed to process refund for BuyingBid {}.", buyingBidId, e);
            throw e;
        }
        ack.acknowledge();
    }
}
