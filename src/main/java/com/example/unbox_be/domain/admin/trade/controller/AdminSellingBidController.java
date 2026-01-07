package com.example.unbox_be.domain.admin.trade.controller;

import com.example.unbox_be.domain.admin.trade.controller.api.AdminSellingBidApi;
import com.example.unbox_be.domain.admin.trade.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.domain.admin.trade.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_be.domain.admin.trade.service.AdminSellingBidService;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
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
@RequestMapping("/api/admin/bids/selling")
@RequiredArgsConstructor
public class AdminSellingBidController implements AdminSellingBidApi {

    private final AdminSellingBidService adminSellingBidService;

    // 판매 입찰 목록 조회
    @GetMapping
    @PreAuthorize("hasAnyRole('MASTER','MANAGER','INSPECTOR')")
    public CustomApiResponse<Page<AdminSellingBidListResponseDto>> getSellingBids(
            @ModelAttribute SellingBidSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminSellingBidListResponseDto> result = adminSellingBidService.getSellingBids(condition, limited);
        return CustomApiResponse.success(result);
    }

    // 판매 입찰 삭제
    @DeleteMapping("/{sellingId}")
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')") // 검수자(INSPECTOR)는 삭제 권한 제외
    public CustomApiResponse<Void> deleteSellingBid(
            @PathVariable UUID sellingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) { // CustomUserDetails로 직접 받아서 타입 안정성 확보
        
        adminSellingBidService.deleteSellingBid(sellingId, userDetails.getUsername());
        return CustomApiResponse.success(null);
    }
}
