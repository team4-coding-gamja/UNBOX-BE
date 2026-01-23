package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


import com.example.unbox_trade.trade.presentation.dto.internal.LowestPriceResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "[내부] 판매 입찰 관리", description = "내부 시스템용 판매 입찰 API")
@RestController
@RequestMapping("/internal/bids/selling")
@RequiredArgsConstructor
public class SellingBidInternalController {

    private final SellingBidService sellingBidService;

    // ✅ 판매 글 조회 (장바구니용)
    @GetMapping("/{id}/for-cart")
    public SellingBidForCartInfoResponse getSellingBidForCart(@PathVariable UUID id) {
        return sellingBidService.getSellingBidForCart(id);
    }

    // ✅ 판매 글 조회 (주문용)
    @GetMapping("/{id}/for-order")
    public SellingBidForOrderInfoResponse getSellingBidForOrder(@PathVariable UUID id) {
        return sellingBidService.getSellingBidForOrder(id);
    }

    // ✅ 판매 입찰 선점 (주문용: LIVE → RESERVED)
    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserveSellingBid(@PathVariable UUID id, @RequestParam String updatedBy) {
        sellingBidService.reserveSellingBid(id, updatedBy);
        return ResponseEntity.ok().build();
    }
    // ✅ 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @PostMapping("/{id}/sold")
    public ResponseEntity<Void> soldSellingBid(@PathVariable UUID id, @RequestParam String updatedBy) {
        sellingBidService.soldSellingBid(id, updatedBy);
        return ResponseEntity.ok().build();
    }
    // ✅ 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @PostMapping("/{id}/live")
    public ResponseEntity<Void> liveSellingBid(@PathVariable UUID id, @RequestParam String updatedBy) {
        sellingBidService.liveSellingBid(id, updatedBy);
        return ResponseEntity.ok().build();
    }

    // ✅ 상품 옵션별 최저가 조회 (Internal)
    @Operation(summary = "상품 옵션별 최저가 조회", description = "상품 옵션 ID로 LIVE 상태인 판매 입찰 중 최저가를 조회합니다.")
    @GetMapping("/product-option/{productOptionId}/lowest-price")
    public LowestPriceResponseDto getLowestPrice(
            @PathVariable UUID productOptionId) {
        return sellingBidService.getLowestPrice(productOptionId);
    }
}
