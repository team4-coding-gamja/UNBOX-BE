package com.example.unbox_be.domain.admin.product.controller;

import com.example.unbox_be.domain.admin.product.controller.api.AdminProductApi;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.admin.product.service.AdminProductService;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.CustomApiResponse;
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
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController implements AdminProductApi {

    private final AdminProductService adminProductService;

    // ✅ 상품 목록 조회 (검색 + 페이징)
    @GetMapping
    public CustomApiResponse<Page<AdminProductListResponseDto>> getProducts(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        return CustomApiResponse.success(adminProductService.getProducts(brandId, category, keyword, limited));
    }

    // ✅ 상품 등록
    @PostMapping
    public CustomApiResponse<AdminProductCreateResponseDto> createProduct(
            @RequestBody @Valid AdminProductCreateRequestDto requestDto) {
        AdminProductCreateResponseDto result = adminProductService.createProduct(requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 상품 수정
    @PatchMapping("/{productId}")
    public CustomApiResponse<AdminProductUpdateResponseDto> updateProduct(
            @PathVariable UUID productId,
            @RequestBody @Valid AdminProductUpdateRequestDto requestDto) {
        AdminProductUpdateResponseDto result = adminProductService.updateProduct(productId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 상품 삭제
    @DeleteMapping("/{productId}")
    public CustomApiResponse<Void> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        String deletedBy = userDetails.getUsername();
        adminProductService.deleteProduct(productId, deletedBy);
        return CustomApiResponse.success(null);
    }
}
