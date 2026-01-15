package com.example.unbox_be.common.client.trade;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// @FeignClient(name = "trade-service")
public interface TradeClient {

    @GetMapping("/internal/bids/selling/{id}/for-cart")
    SellingBidForCartInfoResponse getSellingBidForCart (@PathVariable UUID id);
}
