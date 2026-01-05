package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.ProductRequestRequestDto;
import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;

public interface ProductRequestService {

    // ✅ 상품 등록 요청 생성
    ProductRequestResponseDto createProductRequest(Long userId, ProductRequestRequestDto requestDto);
}
