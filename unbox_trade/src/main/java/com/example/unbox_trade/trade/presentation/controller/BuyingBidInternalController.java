package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.application.service.BuyingBidInternalService;
import com.example.unbox_trade.trade.presentation.dto.internal.BuyingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.HighestPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/bids/buying")
@RequiredArgsConstructor
public class BuyingBidInternalController {

    private final BuyingBidInternalService buyingBidInternalService;

    // ✅ 구매 글 조회 (주문용)
    @GetMapping("/{buyingBidId}/order-info")
    public BuyingBidForOrderInfoResponse getBuyingBidForOrder(@PathVariable UUID buyingBidId) {
        return buyingBidInternalService.getBuyingBidForOrder(buyingBidId);
    }

    // ✅ 구매 입찰 선점 (주문용: LIVE → RESERVED)
    @PostMapping("/{buyingBidId}/reserve")
    public void reserveBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.reserveBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 구매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    // 비동기로 처리될 수 있지만, 비상시/테스트용 API 제공
    @PostMapping("/{buyingBidId}/sold")
    public void soldBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.soldBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 구매 입찰 만료 처리 (주문 취소 시: RESERVED → CANCELLED)
    @PostMapping("/{buyingBidId}/expire")
    public void expireBuyingBid(@PathVariable UUID buyingBidId) {
        buyingBidInternalService.expireBuyingBid(buyingBidId);
    }

    // ✅ 구매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @PostMapping("/{buyingBidId}/live")
    public void liveBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.liveBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 상품 옵션별 최고가 조회 (Internal)
    @GetMapping("/product-option/{productOptionId}/highest-price")
    public HighestPriceResponseDto getHighestPrice(
            @PathVariable UUID productOptionId) {
        return buyingBidInternalService.getHighestPrice(productOptionId);
    }
}