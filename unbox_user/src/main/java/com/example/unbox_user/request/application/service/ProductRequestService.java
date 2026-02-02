package com.example.unbox_user.request.application.service;

import com.example.unbox_user.request.presentation.dto.request.ProductRequestRequestDto;
import com.example.unbox_user.request.presentation.dto.response.ProductRequestResponseDto;

public interface ProductRequestService {

    // ✅ 상품 등록 요청 생성
    ProductRequestResponseDto createProductRequest(Long userId, ProductRequestRequestDto requestDto);
}
