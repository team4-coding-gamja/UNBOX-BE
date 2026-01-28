package com.example.unbox_payment.common.client.trade;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "unbox-trade", url = "${trade-service.url}", path = "/trade")
public interface TradeClient {

    // ✅ 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @PostMapping("/internal/bids/selling/{id}/sold")
    void soldSellingBid(@PathVariable("id") UUID id, @RequestParam("updatedBy") String updatedBy);

    // ✅ 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @PostMapping("/internal/bids/selling/{id}/live")
    void liveSellingBid(@PathVariable("id") UUID id, @RequestParam("updatedBy") String updatedBy);
}
