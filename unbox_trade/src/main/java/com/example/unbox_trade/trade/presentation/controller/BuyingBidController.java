package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_trade.trade.application.service.BuyingBidService;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidCreateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidDetailResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidListResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidsPriceUpdateResponseDto;
import com.example.unbox_trade.trade.presentation.controller.api.BuyingBidApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bids/buying")
@RequiredArgsConstructor
public class BuyingBidController implements BuyingBidApi {

    private final BuyingBidService buyingBidService;

    // ✅ 구매 입찰 생성
    @PostMapping
    public CustomApiResponse<BuyingBidCreateResponseDto> createBuyingBid(
            @Valid @RequestBody BuyingBidCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BuyingBidCreateResponseDto response = buyingBidService.createBuyingBid(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(response);
    }

    // ✅ 구매 입찰 취소
    @DeleteMapping("/{buyingId}")
    public CustomApiResponse<Void> cancelBuyingBid(
            @PathVariable UUID buyingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String deleteBy = userDetails.getUsername();
        buyingBidService.cancelBuyingBid(buyingId, userDetails.getUserId(), deleteBy);
        return CustomApiResponse.successWithNoData();
    }

    // ✅ 구매 입찰 가격 수정
    @PatchMapping("/{buyingId}/price")
    public CustomApiResponse<BuyingBidsPriceUpdateResponseDto> updatePrice(
            @PathVariable UUID buyingId,
            @Valid @RequestBody BuyingBidsPriceUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BuyingBidsPriceUpdateResponseDto response = buyingBidService.updateBuyingBidPrice(buyingId, requestDto,
                userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    // ✅ 구매 입찰 단건 조회
    @GetMapping("/{buyingId}")
    public CustomApiResponse<BuyingBidDetailResponseDto> getBuyingBidDetail(
            @PathVariable UUID buyingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BuyingBidDetailResponseDto response = buyingBidService.getBuyingBidDetail(buyingId, userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    // ✅ 내 구매 입찰 목록 조회
    @GetMapping("/my")
    public CustomApiResponse<Slice<BuyingBidListResponseDto>> getMyBuyingBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Slice<BuyingBidListResponseDto> response = buyingBidService.getMyBuyingBids(userDetails.getUserId(), limited);
        return CustomApiResponse.success(response);
    }
}
