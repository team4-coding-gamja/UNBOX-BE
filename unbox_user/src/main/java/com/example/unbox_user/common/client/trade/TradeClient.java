package com.example.unbox_user.common.client.trade;

import com.example.unbox_user.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_user.common.client.trade.dto.SellingBidForOrderInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "trade-service", url = "${trade-service.url}")
public interface TradeClient {

    // ✅ 판매 글 조회 (장바구니용)
    @GetMapping("/internal/bids/selling/{id}/for-cart")
    SellingBidForCartInfoResponse getSellingBidForCart (@PathVariable UUID id);

    // ✅ 판매 글 조회 (주문용)
    @GetMapping("/internal/bids/selling/{id}/for-order")
    SellingBidForOrderInfoResponse getSellingBidForOrder (@PathVariable UUID id);

    // ✅ 판매 입찰 선점 (주문용: LIVE → RESERVED)
    @PostMapping("/internal/bids/selling/{id}/reserve")
    void reserveSellingBid(@PathVariable UUID id, @RequestParam String updatedBy);

    // ✅ 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @PostMapping("/internal/bids/selling/{id}/sold")
    void soldSellingBid(@PathVariable UUID id, @RequestParam String updatedBy);

    // ✅ 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @PostMapping("/internal/bids/selling/{id}/live")
    void liveSellingBid(@PathVariable UUID id, @RequestParam String updatedBy);
}
