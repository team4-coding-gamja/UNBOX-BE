package com.example.unbox_be.common.client.trade;

import com.example.unbox_be.common.client.trade.dto.SellingBidForCartInfoResponse;
import com.example.unbox_be.common.client.trade.dto.SellingBidForOrderInfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

// @FeignClient(name = "trade-service")
public interface TradeClient {

    @GetMapping("/internal/bids/selling/{id}/for-cart")
    SellingBidForCartInfoResponse getSellingBidForCart (@PathVariable UUID id);

    @GetMapping("/internal/bids/selling/{id}/for-order")
    SellingBidForOrderInfoResponse getSellingBidForOrder (@PathVariable UUID id);

    // ✅ 추가된 메서드: 판매 입찰 점유 (상태 변경)
    // DB 상태를 변경하므로 PostMapping 사용
    @PostMapping("/internal/bids/selling/{id}/occupy")
    void occupySellingBid(@PathVariable UUID id);
}
