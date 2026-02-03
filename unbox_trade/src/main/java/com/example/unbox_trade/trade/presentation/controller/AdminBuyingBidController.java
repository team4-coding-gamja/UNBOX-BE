package com.example.unbox_trade.trade.presentation.controller;

import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_trade.trade.application.service.AdminBuyingBidService;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidSearchCondition;
import com.example.unbox_trade.trade.presentation.dto.response.AdminBuyingBidListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bids/buying")
@RequiredArgsConstructor
public class AdminBuyingBidController {

    private final AdminBuyingBidService adminBuyingBidService;

    // 구매 입찰 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('MASTER','MANAGER','INSPECTOR')")
    public CustomApiResponse<Page<AdminBuyingBidListResponseDto>> getBuyingBids(
            @ModelAttribute BuyingBidSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminBuyingBidListResponseDto> result = adminBuyingBidService.getBuyingBids(condition, limited);
        return CustomApiResponse.success(result);
    }

    // 구매 입찰 삭제
    @DeleteMapping("/{buyingBidId}")
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')") // 검수자(INSPECTOR)는 삭제 권한 제외
    public CustomApiResponse<Void> deleteBuyingBid(
            @PathVariable UUID buyingBidId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminBuyingBidService.deleteBuyingBid(buyingBidId, userDetails.getUsername());
        return CustomApiResponse.success(null);
    }
}
