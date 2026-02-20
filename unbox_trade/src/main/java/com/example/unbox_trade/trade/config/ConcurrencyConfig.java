package com.example.unbox_trade.trade.config;

import com.example.unbox_trade.trade.application.service.purchase.CachedDistributedPurchaseService;
import com.example.unbox_trade.trade.application.service.purchase.DistributedNextBestPurchaseService;
import com.example.unbox_trade.trade.application.service.purchase.DistributedPurchaseService;
import com.example.unbox_trade.trade.application.service.purchase.PessimisticPurchaseService;
import com.example.unbox_trade.trade.application.service.purchase.PurchaseService;
import com.example.unbox_trade.trade.application.service.purchase.QueuedPurchaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConcurrencyConfig {

    @Bean
    public PurchaseService purchaseService(
            @Value("${test.concurrency.stage:stage1}") String stage,
            PessimisticPurchaseService s1,
            DistributedPurchaseService s2,
            DistributedNextBestPurchaseService s3,
            CachedDistributedPurchaseService s4,
            QueuedPurchaseService s6
    ) {
        return switch (stage) {
            case "stage1" -> s1;
            case "stage2" -> s2;
            case "stage3" -> s3;
            case "stage4" -> s4;
            case "stage6" -> s6;
            default -> throw new IllegalArgumentException("Unknown stage: " + stage);
        };
    }
}
