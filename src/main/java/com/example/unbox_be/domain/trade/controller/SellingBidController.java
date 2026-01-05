package com.example.unbox_be.domain.trade.controller;

import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<UUID> createSellingBid(@Valid @RequestBody SellingBidRequestDto requestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID savedId = sellingBidService.createSellingBid(userDetails.getUserId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }

    @DeleteMapping("/{sellingId}")
    public ResponseEntity<Void> cancelSellingBid(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        sellingBidService.cancelSellingBid(sellingId, userDetails.getUserId(), userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{sellingId}/price")
    public ResponseEntity<Void> updatePrice(
            @PathVariable UUID sellingId,
            @Valid @RequestBody SellingBidsPriceUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // [변경] email 대신 userId 전달
        sellingBidService.updateSellingBidPrice(sellingId, requestDto.getNewPrice(), userDetails.getUserId(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sellingId}")
    public ResponseEntity<SellingBidResponseDto> getSellingBidDetail(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // [변경] userId 전달
        return ResponseEntity.ok(sellingBidService.getSellingBidDetail(sellingId, userDetails.getUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<Slice<SellingBidResponseDto>> getMySellingBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(
                    size = 3,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        // [변경] userId 전달
        return ResponseEntity.ok(sellingBidService.getMySellingBids(userDetails.getUserId(), pageable));
    }
}