package com.example.unbox_be.domain.admin.brand.controller;

import com.example.unbox_be.domain.admin.brand.controller.api.AdminBrandApi;
import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.admin.brand.service.AdminBrandService;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
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

    // ✅ 브랜드 조회
    @GetMapping
    public ApiResponse<Page<AdminBrandListResponseDto>> getBrands(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<AdminBrandListResponseDto> result = adminBrandService.getBrands(keyword, limited);
        return ApiResponse.success(result);
    }

    // ✅ 브랜드 등록
    @PostMapping
    public ApiResponse<AdminBrandCreateResponseDto> createBrand(
            @RequestBody @Valid AdminBrandCreateRequestDto requestDto) {
        AdminBrandCreateResponseDto result = adminBrandService.createBrand(requestDto);
        return ApiResponse.success(result);
    }

    @PatchMapping("/{brandId}")
    public ApiResponse<AdminBrandUpdateResponseDto> updateBrand(
            @PathVariable UUID brandId,
            @RequestBody @Valid AdminBrandUpdateRequestDto requestDto) {
        AdminBrandUpdateResponseDto result = adminBrandService.updateBrand(brandId, requestDto);
        return ApiResponse.success(result);
    }

    // ✅ 브랜드 삭제
    @DeleteMapping("/{brandId}")
    public ApiResponse<Void> deleteBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId) {
        String deletedBy = userDetails.getUsername();
        adminBrandService.deleteBrand(brandId, deletedBy);
        return ApiResponse.success(null);
    }
}