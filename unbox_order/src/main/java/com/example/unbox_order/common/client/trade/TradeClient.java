package com.example.unbox_order.common.client.trade;

import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
    name = "unbox-trade",
    url = "${trade-service.url}",
    fallback = TradeClientFallback.class,
    path = "/trade"
)
public interface TradeClient {

    @GetMapping("/internal/bids/selling/{sellingBidId}/for-order")
    SellingBidForOrderResponse getSellingBidForOrder(@PathVariable("sellingBidId") UUID sellingBidId);

    @PostMapping("/internal/bids/selling/{sellingBidId}/reserve")
    void reserveSellingBid(@PathVariable("sellingBidId") UUID sellingBidId, @RequestParam("updatedBy") String updatedBy);

    @PostMapping("/internal/bids/selling/{sellingBidId}/sold")
    void soldSellingBid(@PathVariable("sellingBidId") UUID sellingBidId, @RequestParam("updatedBy") String updatedBy);

    @PostMapping("/internal/bids/selling/{sellingBidId}/live")
    void liveSellingBid(@PathVariable("sellingBidId") UUID sellingBidId, @RequestParam("updatedBy") String updatedBy);
}
