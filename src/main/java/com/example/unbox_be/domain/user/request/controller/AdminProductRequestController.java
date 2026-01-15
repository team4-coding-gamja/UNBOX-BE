package com.example.unbox_be.domain.user.request.controller;

import com.example.unbox_be.domain.user.request.controller.api.AdminProductRequestApi;
import com.example.unbox_be.domain.user.request.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.user.request.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.user.request.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.user.request.service.AdminProductRequestService;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/product-requests")
public class AdminProductRequestController implements AdminProductRequestApi {

    private final AdminProductRequestService adminProductRequestService;

    // ✅ 상품 요청 목록 조회
    @GetMapping
    public CustomApiResponse<Page<AdminProductRequestListResponseDto>> getProductRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminProductRequestListResponseDto> result = adminProductRequestService.getProductRequests(limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 상품 요청 상태 변경
    @PatchMapping("/{productRequestId}/status")
    public CustomApiResponse<AdminProductRequestUpdateResponseDto> updateProductRequestStatus(
            @PathVariable UUID productRequestId,
            @RequestBody AdminProductRequestUpdateRequestDto requestDto) {
        AdminProductRequestUpdateResponseDto result = adminProductRequestService.updateProductRequestStatus(productRequestId, requestDto);
        return CustomApiResponse.success(result);
    }
}
