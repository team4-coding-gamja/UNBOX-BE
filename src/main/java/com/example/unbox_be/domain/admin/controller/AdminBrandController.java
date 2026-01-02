package com.example.unbox_be.domain.admin.controller;

import com.example.unbox_be.domain.admin.controller.Api.AdminBrandApi;
import com.example.unbox_be.domain.admin.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.service.AdminBrandService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController implements AdminBrandApi {

    private final AdminBrandService adminBrandService;

    // ✅ 브랜드 등록 API
    @PostMapping
    public ApiResponse<AdminBrandCreateResponseDto> createBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminBrandCreateRequestDto adminBrandCreateRequestDto) {
        AdminBrandCreateResponseDto result = adminBrandService.createBrand(userDetails.getUsername(), adminBrandCreateRequestDto);
        return ApiResponse.success(result);
    }

    // ✅ 브랜드 삭제 API
    @DeleteMapping("/{brandId}")
    public ApiResponse<Void> deleteBrand(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID brandId) {
        adminBrandService.deleteBrand(userDetails.getUsername(), brandId);
        return ApiResponse.success(null);
    }
}
