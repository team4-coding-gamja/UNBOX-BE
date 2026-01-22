package com.example.unbox_order.common.client.trade;

import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "unbox-trade", url = "${trade-service.url}")
public interface TradeClient {

    @GetMapping("/internal/bids/selling/{sellingBidId}/order-info")
    SellingBidForOrderResponse getSellingBidForOrder(@PathVariable("sellingBidId") UUID sellingBidId);

    @PatchMapping("/internal/bids/selling/{sellingBidId}/status")
    void updateSellingBidStatus(@PathVariable("sellingBidId") UUID sellingBidId, @RequestParam("status") String status);
}
