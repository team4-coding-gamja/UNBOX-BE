package com.example.unbox_order.common.client.trade;

import com.example.unbox_order.common.client.trade.dto.BuyingBidForOrderResponse;
import com.example.unbox_order.common.client.trade.dto.SellingBidForOrderResponse;
import com.example.unbox_common.response.CustomApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "unbox-trade", url = "${trade-service.url}", path = "/trade", fallback = TradeClientFallback.class)
public interface TradeClient {

        @GetMapping("/internal/bids/selling/{sellingBidId}/for-order")
        SellingBidForOrderResponse getSellingBidForOrder(@PathVariable("sellingBidId") UUID sellingBidId);

        @PostMapping("/internal/bids/selling/{sellingBidId}/reserve")
        void reserveSellingBid(@PathVariable("sellingBidId") UUID sellingBidId,
                        @RequestParam("updatedBy") String updatedBy);

        @PostMapping("/internal/bids/selling/{sellingBidId}/sold")
        void soldSellingBid(@PathVariable("sellingBidId") UUID sellingBidId,
                        @RequestParam("updatedBy") String updatedBy);

        @PostMapping("/internal/bids/selling/{sellingBidId}/live")
        void liveSellingBid(@PathVariable("sellingBidId") UUID sellingBidId,
                        @RequestParam("updatedBy") String updatedBy);

        // ✅ Buying Bid Methods
        @GetMapping("/internal/bids/buying/{buyingBidId}/order-info")
        CustomApiResponse<BuyingBidForOrderResponse> getBuyingBidForOrder(
                        @PathVariable("buyingBidId") UUID buyingBidId);

        @PostMapping("/internal/bids/buying/{buyingBidId}/reserve")
        void reserveBuyingBid(@PathVariable("buyingBidId") UUID buyingBidId,
                        @RequestParam("updatedBy") String updatedBy);

        @PostMapping("/internal/bids/buying/{buyingBidId}/sold")
        void soldBuyingBid(@PathVariable("buyingBidId") UUID buyingBidId, @RequestParam("updatedBy") String updatedBy);

        @PostMapping("/internal/bids/buying/{buyingBidId}/live")
        void liveBuyingBid(@PathVariable("buyingBidId") UUID buyingBidId, @RequestParam("updatedBy") String updatedBy);

        @PostMapping("/internal/bids/buying/{buyingBidId}/reset-match")
        void resetMatchedBid(@PathVariable UUID buyingBidId);
}
