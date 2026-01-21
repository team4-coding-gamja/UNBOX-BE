package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.request.UpdateSellingStatusRequestDto;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/internal/bids/selling")
@RequiredArgsConstructor
public class SellingBidInternalController {

    private final SellingBidService sellingBidService;

    // ✅ 판매 글 조회 (장바구니용)
    @GetMapping("/{id}/for-cart")
    public SellingBidForCartInfoResponse getSellingBidForCart(@PathVariable("id") UUID id) {
        return sellingBidService.getSellingBidForCart(id);
    }

    // ✅ 판매 글 조회 (주문용)
    @GetMapping("/{id}/for-order")
    public SellingBidForOrderInfoResponse getSellingBidForOrder(@PathVariable("id") UUID id) {
        return sellingBidService.getSellingBidForOrder(id);
    }

    // ✅ 주문 상태 변경 (주문용)
    @PostMapping("/{id}/occupy")
    public ResponseEntity<Void> occupySellingBid(@PathVariable("id") UUID id) {
        sellingBidService.occupySellingBid(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateSellingBidStatus(
            @PathVariable UUID id,
            @RequestBody UpdateSellingStatusRequestDto request) {
        sellingBidService.updateSellingBidStatus(id, request.status().name(), request.updatedBy());
        return ResponseEntity.ok().build();
    }
}
