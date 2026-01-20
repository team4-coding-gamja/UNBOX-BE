package com.example.unbox_user.user.request.service;

import com.example.unbox_user.user.request.dto.request.ProductRequestRequestDto;
import com.example.unbox_user.user.request.dto.response.ProductRequestResponseDto;

public interface ProductRequestService {

    // ✅ 상품 등록 요청 생성
    ProductRequestResponseDto createProductRequest(Long userId, ProductRequestRequestDto requestDto);
}
