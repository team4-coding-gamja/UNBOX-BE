package com.example.unbox_product.product.application.event.producer;

import com.example.unbox_common.event.product.BrandDeletedEvent;
import com.example.unbox_common.event.product.ProductDeletedEvent;
import com.example.unbox_common.event.product.ProductOptionDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_PRODUCT = "product-events";

    public void publishProductDeleted(ProductDeletedEvent event) {
        log.info("Publishing ProductDeletedEvent: productId={}, deletedOptionIds={}", event.productId(), event.deletedOptionIds());
        kafkaTemplate.send(TOPIC_PRODUCT, event.productId().toString(), event);
    }

    public void publishBrandDeleted(BrandDeletedEvent event) {
        log.info("Publishing BrandDeletedEvent: BrandId={}, deletedProductIds={}, deletedOptionIds={}", event.brandId(),event.deletedProductIds(), event.deletedOptionIds());
        kafkaTemplate.send(TOPIC_PRODUCT, event.brandId().toString(), event);
    }

    public void publishProductOptionDeleted(ProductOptionDeletedEvent event) {
        log.info("Publishing ProductDeletedEvent: productOptionId={}", event.productOptionId());
        kafkaTemplate.send(TOPIC_PRODUCT, event.productOptionId().toString(), event);
    }
}
