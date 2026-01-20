package com.example.unbox_be.common.client.trade;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// @FeignClient(name = "trade-service")
public interface TradeClient {

    // ✅ 판매 글 조회 (장바구니용)
    @GetMapping("/internal/bids/selling/{id}/for-cart")
    SellingBidForCartInfoResponse getSellingBidForCart (@PathVariable UUID id);

    // ✅ 판매 글 조회 (주문용)
    @GetMapping("/internal/bids/selling/{id}/for-order")
    SellingBidForOrderInfoResponse getSellingBidForOrder (@PathVariable UUID id);

    // ✅ 주문 상태 변경 (주문용)
    @PostMapping("/internal/bids/selling/{id}/occupy")
    void occupySellingBid(@PathVariable UUID id);

    // ✅ 판매입찰 상태변경 (결제용)
    @PatchMapping("/internal/bids/selling/{id}/status")
    void updateSellingBidStatus(@PathVariable UUID id, @RequestParam("status") String status, @RequestParam("updatedBy") String updatedBy);
}
