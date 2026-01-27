package com.example.unbox_product.common.client.trade;

import com.example.unbox_product.common.client.trade.dto.LowestPriceResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;
import java.util.List;

@FeignClient(name = "unbox-trade", url = "${trade-service.url}", path = "/trade")
public interface TradeClient {

    // ✅ 상품 옵션별 최저가 조회 (Internal)
    @GetMapping("/internal/bids/selling/product-option/{productOptionId}/lowest-price")
    LowestPriceResponseDto getLowestPrice(@PathVariable("productOptionId") UUID productOptionId);

    @PostMapping("/internal/bids/selling/product-options/lowest-prices")
    List<LowestPriceResponseDto> getLowestPrices(@RequestBody List<UUID> productOptionIds);
}
