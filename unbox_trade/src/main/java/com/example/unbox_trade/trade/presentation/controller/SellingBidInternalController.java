package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.application.service.SellingBidService;
import com.example.unbox_common.error.ErrorResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.LockAcquisitionFailedException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Object> reserveSellingBid(@PathVariable UUID id, @RequestParam String updatedBy) {
        try {
            sellingBidService.reserveSellingBid(id, updatedBy);
            return ResponseEntity.ok().build();
        } catch (LockAcquisitionFailedException | CustomException e) {
            // 락 획득 실패(누군가 결제 중) 또는 이미 판매됨(CustomException) -> Next Best Offer 제안
            try {
                // 1. 해당 입찰의 상품 옵션 ID 조회
                // (Controller에서 Service를 통해 조회하거나, 예외 상황이므로 여기서 간단히 조회)
                // getSellingBidDetail 내부적으로 본인 확인 로직이 있어서, 내부 시스템용 조회 메서드가 필요할 수 있음.
                // 여기서는 getSellingBidForOrder를 재활용 (productOptionId 포함되어 있음)
                SellingBidForOrderInfoResponse bidInfo = sellingBidService.getSellingBidForOrder(id);
                
                // 2. 해당 옵션의 최저가 조회
                LowestPriceResponseDto nextBestOffer = sellingBidService.getLowestPrice(bidInfo.getProductOptionId());
                
                // 3. 409 Conflict 반환 (Next Offer 데이터 포함)
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ErrorResponse.of(
                                HttpStatus.CONFLICT.value(), 
                                "현재 상품은 결제 진행 중이거나 판매 완료되었습니다. 다음 최저가로 구매하시겠습니까?", 
                                nextBestOffer));
            } catch (Exception innerEx) {
                // 조회 실패 시 원래 에러 던짐
                throw e;
            }
        }
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
