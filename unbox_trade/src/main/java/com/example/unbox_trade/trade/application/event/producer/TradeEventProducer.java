package com.example.unbox_trade.trade.application.event.producer;

import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC_TRADE = "trade-events";

    public void publishTradePriceChanged(TradePriceChangedEvent event) {
        log.info("Publishing TradePriceChangedEvent: productId={}, productOptionId={}, newLowestPrice={}", event.productId(), event.optionId(), event.newLowestPrice());
        kafkaTemplate.send(TOPIC_TRADE, event.optionId().toString(), event );
    }
}
