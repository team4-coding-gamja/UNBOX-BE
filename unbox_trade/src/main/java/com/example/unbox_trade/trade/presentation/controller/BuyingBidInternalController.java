package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.application.service.BuyingBidInternalService;
import com.example.unbox_trade.trade.presentation.dto.internal.BuyingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.HighestPriceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "[내부] 구매 입찰 관리", description = "내부 시스템용 구매 입찰 API")
@RestController
@RequestMapping("/internal/bids/buying")
@RequiredArgsConstructor
public class BuyingBidInternalController {

    private final BuyingBidInternalService buyingBidInternalService;

    // ✅ 구매 글 조회 (주문용)
    @Operation(summary = "구매 글 조회 (주문용)", description = "주문 처리를 위해 구매 입찰 정보를 조회합니다.")
    @GetMapping("/{buyingBidId}/order-info")
    public BuyingBidForOrderInfoResponse getBuyingBidForOrder(@PathVariable UUID buyingBidId) {
        return buyingBidInternalService.getBuyingBidForOrder(buyingBidId);
    }

    // ✅ 구매 입찰 선점 (주문용: LIVE → RESERVED)
    @Operation(summary = "구매 입찰 선점", description = "주문 시작 시 구매 입찰의 상태를 LIVE에서 RESERVED로 변경합니다.")
    @PostMapping("/{buyingBidId}/reserve")
    public void reserveBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.reserveBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 구매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @Operation(summary = "구매 입찰 완료 처리", description = "결제 완료 후 구매 입찰의 상태를 RESERVED에서 SOLD로 변경합니다.")
    @PostMapping("/{buyingBidId}/sold")
    public void soldBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.soldBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 구매 입찰 만료 처리 (주문 취소 시: RESERVED → CANCELLED)
    @Operation(summary = "구매 입찰 만료 처리", description = "주문 취소 시 구매 입찰의 상태를 RESERVED에서 CANCELLED로 변경합니다.")
    @PostMapping("/{buyingBidId}/expire")
    public void expireBuyingBid(@PathVariable UUID buyingBidId) {
        buyingBidInternalService.expireBuyingBid(buyingBidId);
    }

    // ✅ 구매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @Operation(summary = "구매 입찰 복구", description = "결제 실패 또는 취소 시 구매 입찰의 상태를 RESERVED에서 LIVE로 복구합니다.")
    @PostMapping("/{buyingBidId}/live")
    public void liveBuyingBid(@PathVariable UUID buyingBidId,
            @RequestParam(value = "updatedBy", defaultValue = "SYSTEM") String updatedBy) {
        buyingBidInternalService.liveBuyingBid(buyingBidId, updatedBy);
    }

    // ✅ 상품 옵션별 최고가 조회 (Internal)
    @Operation(summary = "상품 옵션별 최고가 조회", description = "특정 상품 옵션의 최고가 구매 입찰 가격을 조회합니다.")
    @GetMapping("/product-option/{productOptionId}/highest-price")
    public HighestPriceResponseDto getHighestPrice(@PathVariable UUID productOptionId) {
        return buyingBidInternalService.getHighestPrice(productOptionId);
    }

    // ✅ 상품 옵션별 최고가 조회 (Internal)
    @Operation(summary = "상품 옵션별 최고가 조회 (배치)", description = "여러 상품 옵션의 최고가 구매 입찰 가격을 한꺼번에 조회합니다.")
    @PostMapping("/product-options/highest-price")
    public List<HighestPriceResponseDto> getHighestPrices(@RequestBody List<UUID> productOptionIds) {
        return buyingBidInternalService.getHighestPrices(productOptionIds);
    }

    // ✅ match -> live
    @PostMapping("/{buyingBidId}/reset-match")
    public void resetMatchedBid(@PathVariable UUID buyingBidId) {
        buyingBidInternalService.resetMatchedBid(buyingBidId);
    }
}