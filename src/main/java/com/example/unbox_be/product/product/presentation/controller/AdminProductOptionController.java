package com.example.unbox_be.product.product.presentation.controller;

import com.example.unbox_be.product.product.presentation.controller.api.AdminProductOptionApi;
import com.example.unbox_be.product.product.presentation.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_be.product.product.application.service.AdminProductOptionService;
import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/options")
@RequiredArgsConstructor
public class AdminProductOptionController implements AdminProductOptionApi {

    private final AdminProductOptionService adminProductService;

    // ✅ 상품 옵션 목록 조회
    @GetMapping
    public CustomApiResponse<Page<AdminProductOptionListResponseDto>> getProductOptions(
            @PathVariable UUID productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        return CustomApiResponse.success(adminProductService.getProductOptions(productId, limited));
    }

    // ✅ 상품 옵션 등록
    @PostMapping
    public CustomApiResponse<AdminProductOptionCreateResponseDto> createProductOption(
            @PathVariable UUID productId,
            @RequestBody @Valid AdminProductOptionCreateRequestDto requestDto) {
        AdminProductOptionCreateResponseDto result = adminProductService.createProductOption(productId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 상품 옵션 삭제
    @DeleteMapping("{optionId}")
    public CustomApiResponse<Void> deleteProductOption(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId,
            @PathVariable UUID optionId) {
        String deletedBy = userDetails.getUsername();
        adminProductService.deleteProductOption(productId, optionId, deletedBy);
        return CustomApiResponse.success(null);
    }
}
