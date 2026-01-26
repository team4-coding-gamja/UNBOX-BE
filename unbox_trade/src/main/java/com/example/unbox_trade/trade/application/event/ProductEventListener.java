package com.example.unbox_trade.trade.application.event;

import com.example.unbox_common.event.product.BrandDeletedEvent;
import com.example.unbox_common.event.product.ProductDeletedEvent;
import com.example.unbox_common.event.product.ProductOptionDeletedEvent;
import com.example.unbox_trade.trade.application.service.AdminSellingBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final AdminSellingBidService adminSellingBidService;

    /**
     * ✅ 통합 리스너 (Single Entry Point)
     * 'product-events' 토픽으로 들어오는 모든 메시지를 여기서 받아서
     * 타입에 따라 적절한 로직으로 분기합니다.
     */
    @KafkaListener(topics = "product-events", groupId = "trade-group")
    public void handleProductEvents(ConsumerRecord<String, Object> record, Acknowledgment ack) {

        Object event = record.value();
        String eventType = event.getClass().getSimpleName();

        log.info("[Trade] Received Event: {}", eventType);

        try {
            // 1. 브랜드 삭제 이벤트
            if (event instanceof BrandDeletedEvent brandDeletedEvent) {
                log.info("Processing Brand Delete: brandId={}", brandDeletedEvent.brandId());
                adminSellingBidService.deleteSellingBidsByOptionIds(brandDeletedEvent.deletedOptionIds(), "PRODUCT_EVENT");
                log.info("Finished Brand Delete.");
            }
            // 2. 상품 삭제 이벤트
            else if (event instanceof ProductDeletedEvent productDeletedEvent) {
                log.info("Processing Product Delete: productId={}", productDeletedEvent.productId());
                adminSellingBidService.deleteSellingBidsByOptionIds(productDeletedEvent.deletedOptionIds(), "PRODUCT_EVENT");
                log.info("Finished Product Delete.");
            }
            // 3. 옵션 단건 삭제 이벤트
            else if (event instanceof ProductOptionDeletedEvent productOptionDeletedEvent) {
                log.info("Processing Option Delete: optionId={}", productOptionDeletedEvent.productOptionId());
                adminSellingBidService.deleteSellingBidByOptionId(productOptionDeletedEvent.productOptionId(), "PRODUCT_EVENT");
                log.info("Finished ProductOption Delete.");
            }
            // 4. 모르는 이벤트
            else {
                log.warn("Ignored unknown event type: {}", eventType);
                // 모르는 이벤트라도 처리는 된 것이므로 ack를 할지 말지는 정책 결정 필요.
                // 보통은 에러를 내지 않고 넘어가야 컨슈머가 멈추지 않음.
            }

            // ✅ [필수] 성공적으로 분기 처리가 끝났으면 커밋!
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process event: {}", eventType, e);
            // 여기서 예외를 던져야 Kafka 설정에 따라 재시도(Retry)를 하거나 DLQ로 빠짐
            throw e;
        }
    }
}