package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.dto.ProductResponseDto;
import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.service.ProductService;
import com.example.unbox_be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductResponseDto>> getProducts(
            @ModelAttribute ProductSearchCondition condition, // 검색 조건 추가
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ){
        // 서비스 메서드 호출
        return ApiResponse.success(productService.getProducts(condition, pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponseDto> getProductById(@PathVariable("productId") UUID id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}
