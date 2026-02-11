package com.example.unbox_product.product.presentation.controller;

import com.example.unbox_product.product.application.service.ProductService;
import com.example.unbox_product.product.application.service.TestProductService;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_product.product.presentation.dto.response.SliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test/products")
@RequiredArgsConstructor
public class TestProductController {

    private final ProductService productService;
    private final TestProductService testProductService;

    // [V1] 기존 로직 Baseline 측정용
    @GetMapping("/v1")
    public CustomApiResponse<Page<ProductListResponseDto>> getProductsV1(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return CustomApiResponse.success(productService.getProducts(brandId, category, keyword, pageable));
    }

    @GetMapping("/v2")
    public CustomApiResponse<SliceResponse<ProductListResponseDto>> getProductsV2(
            @RequestParam(required = false) UUID lastProductId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int size) {
        return CustomApiResponse.success(testProductService.getProductsV2(lastProductId, brandId, category, keyword, size));
    }
}