package com.example.unbox_be.domain.admin.product.controller;

import com.example.unbox_be.domain.admin.product.controller.api.AdminProductApi;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.product.service.AdminProductService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController implements AdminProductApi {

    private final AdminProductService adminProductService;

    // ✅ 상품 등록
    @PostMapping
    public ApiResponse<AdminProductCreateResponseDto> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminProductCreateRequestDto requestDto) {
        AdminProductCreateResponseDto result = adminProductService.createProduct(userDetails.getUsername(), requestDto);
        return ApiResponse.success(result);
    }

    // ✅ 상품 삭제
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        adminProductService.deleteProduct(userDetails.getUsername(), productId);
        return ApiResponse.success(null);
    }

    // ✅ 상품 옵션 등록
    @PostMapping("/{productId}/options")
    public ApiResponse<AdminProductOptionCreateResponseDto> createProductOption(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId,
            @RequestBody @Valid AdminProductOptionCreateRequestDto adminProductOptionCreateRequestDto) {
        AdminProductOptionCreateResponseDto result = adminProductService.createProductOption(userDetails.getUsername(), productId, adminProductOptionCreateRequestDto);
        return ApiResponse.success(result);
    }

    // ✅ 상품 옵션 삭제
    @DeleteMapping("/{productId}/options/{optionId}")
    public ApiResponse<Void> deleteProductOption(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId,
            @PathVariable UUID optionId) {
        adminProductService.deleteProductOption(userDetails.getUsername(), productId, optionId);
        return ApiResponse.success(null);
    }
}
