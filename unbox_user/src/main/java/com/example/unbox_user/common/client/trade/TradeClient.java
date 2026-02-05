package com.example.unbox_user.common.client.trade;

import com.example.unbox_user.common.client.trade.dto.SellingBidForCartInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "trade-service", url = "${trade-service.url}", path = "/trade")
public interface TradeClient {

    // ✅ 판매 글 조회 (장바구니용)
    @GetMapping("/internal/bids/selling/{id}/for-cart")
    SellingBidForCartInfoResponse getSellingBidForCart (@PathVariable UUID id);
}
