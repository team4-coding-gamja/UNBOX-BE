package com.example.unbox_be.trade.presentation.controller;

import com.example.unbox_be.trade.presentation.controller.api.SellingBidApi;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_be.trade.application.service.SellingBidService;
import com.example.unbox_be.common.pagination.PageSizeLimiter;
import com.example.unbox_be.common.response.CustomApiResponse;
import com.example.unbox_be.common.security.auth.CustomUserDetails;
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
@RequestMapping("/api/bids/selling")
@RequiredArgsConstructor
public class SellingBidController implements SellingBidApi {

    private final SellingBidService sellingBidService;

    // ✅ 판매 입찰 생성
    @PostMapping
    public CustomApiResponse<SellingBidCreateResponseDto> createSellingBid(
            @Valid @RequestBody SellingBidCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SellingBidCreateResponseDto response = sellingBidService.createSellingBid(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(response);
    }

    // ✅ 판매 입찰 취소
    @DeleteMapping("/{sellingId}")
    public CustomApiResponse<Void> cancelSellingBid(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String deleteBy = userDetails.getUsername();
        sellingBidService.cancelSellingBid(sellingId, userDetails.getUserId(), deleteBy);
        return CustomApiResponse.successWithNoData();
    }

    // ✅ 판매 입찰 가격 수정
    @PatchMapping("/{sellingId}/price")
    public CustomApiResponse<SellingBidsPriceUpdateResponseDto> updatePrice(
            @PathVariable UUID sellingId,
            @Valid @RequestBody SellingBidsPriceUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SellingBidsPriceUpdateResponseDto response = sellingBidService.updateSellingBidPrice(sellingId, requestDto, userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    // ✅ 판매 입찰 단건 조회
    @GetMapping("/{sellingId}")
    public CustomApiResponse<SellingBidDetailResponseDto> getSellingBidDetail(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SellingBidDetailResponseDto response = sellingBidService.getSellingBidDetail(sellingId,
                userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    // ✅ 내 판매 입찰 목록 조회
    @GetMapping("/my")
    public CustomApiResponse<Slice<SellingBidListResponseDto>> getMySellingBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Slice<SellingBidListResponseDto> response = sellingBidService.getMySellingBids(userDetails.getUserId(),
                limited);
        return CustomApiResponse.success(response);
    }
}