package com.example.unbox_product.common.client.trade;

import com.example.unbox_product.common.client.trade.dto.LowestPriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "unbox-trade", url = "${trade-service.url}")
public interface TradeClient {

    // ✅ 상품 옵션별 최저가 조회 (Internal)
    @GetMapping("/internal/bids/selling/product-option/{productOptionId}/lowest-price")
    LowestPriceResponseDto getLowestPrice(@PathVariable("productOptionId") UUID productOptionId);
}
