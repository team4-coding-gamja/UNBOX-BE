package com.example.unbox_trade.trade.application.event;

import com.example.unbox_trade.trade.application.service.AdminSellingBidService;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_common.event.product.BrandDeletedEvent;
import com.example.unbox_common.event.product.ProductDeletedEvent;
import com.example.unbox_common.event.product.ProductOptionDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component  
@RequiredArgsConstructor
public class TradeEventListener {

    private final AdminSellingBidService adminSellingBidService;
    private final SellingBidService sellingBidService;

    /**
     * ✅ 결제 완료 이벤트 (Kafka)
     * Payment 서비스에서 결제가 완료되면 입찰 상태를 SOLD로 변경합니다.
     */
    @KafkaListener(topics = "payment-events", groupId = "trade-group")
    public void handlePaymentEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        Object event = record.value();

        if (event == null) {
            log.warn("Received null event in TradeEventListener. Key: {}", record.key());
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

    /**
     * ✅ 브랜드 삭제 이벤트
     * 브랜드가 삭제되면 그 아래 딸린 옵션 ID들이 리스트로 넘어옵니다.
     * 이 ID들에 해당하는 판매 입찰을 일괄 삭제합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBrandDeleted(BrandDeletedEvent event) {
        log.info("Handle Brand Deleted in Trade: brandId={}", event.brandId());

        if (event.deletedOptionIds() != null && !event.deletedOptionIds().isEmpty()) {
            adminSellingBidService.deleteSellingBidsByOptionIds(event.deletedOptionIds());
        }
    }


    /**
     * ✅ 상품 삭제 이벤트
     * 상품이 삭제되면 그 아래 딸린 옵션 ID들이 리스트로 넘어옵니다. (ProductDeletedEvent 정의 참고)
     * 마찬가지로 일괄 삭제합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {
        log.info("Handle Product Deleted in Trade: productId={}", event.productId());

        // ⚠️ 수정: productId가 아니라, 이벤트에 담긴 OptionId 리스트를 사용해야 정확합니다.
        // SellingBid는 보통 Product가 아니라 ProductOption에 걸리기 때문입니다.
        if (event.deletedOptionIds() != null && !event.deletedOptionIds().isEmpty()) {
            adminSellingBidService.deleteSellingBidsByOptionIds(event.deletedOptionIds());
        }
    }

    /**
     * ✅ 옵션 단건 삭제 이벤트
     * 옵션 하나가 삭제되면 해당 옵션 ID에 걸린 입찰을 삭제합니다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductOptionDeleted(ProductOptionDeletedEvent event) {
        log.info("Handle Option Deleted in Trade: optionId={}", event.productOptionId());

        adminSellingBidService.deleteSellingBidByOptionId(event.productOptionId());
    }
}