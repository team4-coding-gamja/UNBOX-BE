package com.example.unbox_be.product.product.presentation.controller;

import com.example.unbox_be.product.product.presentation.controller.api.AdminBrandApi;
import com.example.unbox_be.product.product.presentation.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.product.product.presentation.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.product.product.application.service.AdminBrandService;
import com.example.unbox_be.common.pagination.PageSizeLimiter;
import com.example.unbox_be.common.response.CustomApiResponse;
import com.example.unbox_be.common.security.auth.CustomUserDetails;
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
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController implements AdminBrandApi {

    private final AdminBrandService adminBrandService;

    // ✅ 브랜드 목록 조회
    @GetMapping
    public CustomApiResponse<Page<AdminBrandListResponseDto>> getBrands(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminBrandListResponseDto> result = adminBrandService.getBrands(keyword, limited);
        return CustomApiResponse.success(result);
    }

    // ✅ 브랜드 상세 조회
    @GetMapping("/{brandId}")
    public CustomApiResponse<AdminBrandDetailResponseDto> getBrandDetail(
            @PathVariable UUID brandId) {
        AdminBrandDetailResponseDto result = adminBrandService.getBrandDetail(brandId);
        return CustomApiResponse.success(result);
    }

    // ✅ 브랜드 등록
    @PostMapping
    public CustomApiResponse<AdminBrandCreateResponseDto> createBrand(
            @RequestBody @Valid AdminBrandCreateRequestDto requestDto) {
        AdminBrandCreateResponseDto result = adminBrandService.createBrand(requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 브랜드 수정
    @PatchMapping("/{brandId}")
    public CustomApiResponse<AdminBrandUpdateResponseDto> updateBrand(
            @PathVariable UUID brandId,
            @RequestBody @Valid AdminBrandUpdateRequestDto requestDto) {
        AdminBrandUpdateResponseDto result = adminBrandService.updateBrand(brandId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 브랜드 삭제
    @DeleteMapping("/{brandId}")
    public CustomApiResponse<Void> deleteBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId) {
        String deletedBy = userDetails.getUsername();
        adminBrandService.deleteBrand(brandId, deletedBy);
        return CustomApiResponse.success(null);
    }
}