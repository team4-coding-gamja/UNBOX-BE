package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.dto.ProductResponseDto;
import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.service.ProductService;
import com.example.unbox_be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject; // ★ 이 import 필수!
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
            // ▼ 여기에 @ParameterObject 추가! (스웨거야, 이거 필드별로 쪼개서 보여줘!)
            @ParameterObject @ModelAttribute ProductSearchCondition condition,

            // ▼ Pageable 앞에도 붙여주면 page, size, sort 입력창이 깔끔하게 나옵니다.
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable
    ){
        return ApiResponse.success(productService.getProducts(condition, pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponseDto> getProductById(@PathVariable("productId") UUID id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}