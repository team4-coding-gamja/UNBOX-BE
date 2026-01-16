package com.example.unbox_be.product.product.presentation.controller;

import com.example.unbox_be.product.product.presentation.controller.api.ProductApi;
import com.example.unbox_be.product.product.presentation.dto.response.BrandListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.product.product.application.service.ProductService;
import com.example.unbox_be.product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_common.pagination.PageSizeLimiter;
import com.example.unbox_common.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    // ✅ 상품 목록 조회 (검색 + 페이징)
    @GetMapping
    public CustomApiResponse<Page<ProductListResponseDto>> getProducts(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @ParameterObject Pageable pageable) {
        return CustomApiResponse.success(productService.getProducts(brandId, category, keyword, pageable));
    }

    // ✅ 상품 상세 조회
    @GetMapping("/{productId}")
    public CustomApiResponse<ProductDetailResponseDto> getProductDetail(
            @PathVariable UUID productId) {
        ProductDetailResponseDto result = productService.getProductDetail(productId);
        return CustomApiResponse.success(result);
    }

    // ✅ 상품 옵션별 최저가 조회
    @GetMapping("/{productId}/options")
    public CustomApiResponse<List<ProductOptionListResponseDto>> getProductOptions(
            @PathVariable UUID productId) {
        List<ProductOptionListResponseDto> result = productService.getProductOptions(productId);
        return CustomApiResponse.success(result);
    }

    // ✅ 브랜드 전체 조회
    @GetMapping("/brands")
    public CustomApiResponse<List<BrandListResponseDto>> getAllBrands() {
        return CustomApiResponse.success(productService.getAllBrands());
    }

    // ✅ 상품별 리뷰 조회
    @GetMapping("/{productId}/reviews")
    public CustomApiResponse<Page<ReviewListResponseDto>> getReviewsByProduct(
            @PathVariable UUID productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<ReviewListResponseDto> result = productService.getReviewsByProduct(productId, limited);
        return CustomApiResponse.success(result);
    }
}
