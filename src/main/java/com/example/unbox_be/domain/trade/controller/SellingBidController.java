package com.example.unbox_be.domain.trade.controller;

import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bids/selling")
@RequiredArgsConstructor
public class SellingBidController {

    private final SellingBidService sellingBidService;

    //판매 주문 /api/bids/selling
    @PostMapping
    public ResponseEntity<UUID> createSellingBid(@Valid @RequestBody SellingBidRequestDto requestDto) {
        UUID savedId = sellingBidService.createSellingBid(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }

    @DeleteMapping("/{sellingId}")
    public ResponseEntity<Void> cancelSellingBid(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        sellingBidService.cancelSellingBid(sellingId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sellingId}/price")
    public ResponseEntity<Void> updatePrice(
            @PathVariable UUID sellingId,
            @Valid @RequestBody SellingBidsPriceUpdateRequestDto requestDto, // DTO와 검증 추가
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. CustomUserDetails에서 이메일 추출
        String email = userDetails.getUsername();

        // 2. DTO에서 검증된 가격 추출
        Integer newPrice = requestDto.getNewPrice();

        // 3. 서비스 계층 호출
        sellingBidService.updateSellingBidPrice(sellingId, newPrice, email);

        return ResponseEntity.ok().build();
    }
}