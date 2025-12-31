package com.example.unbox_be.domain.product.controller;

import com.example.unbox_be.domain.product.dto.ProductResponseDto;
import com.example.unbox_be.domain.product.service.ProductService;
import com.example.unbox_be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<Page<ProductResponseDto>> getProducts(@PageableDefault(size = 10, sort = "id")Pageable pageable){
        return ApiResponse.success(productService.getAllProducts(pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponseDto> getProductById(@PathVariable("productId") UUID id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}
