package com.example.unbox_be.domain.admin.product.service;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;

import java.util.UUID;

public interface AdminProductService {

    // ✅ 상품 등록
    AdminProductCreateResponseDto createProduct(AdminProductCreateRequestDto requestDto);
    // ✅ 상품 삭제
    void deleteProduct(UUID productId);
    // ✅ 상품 옵션 등록
    AdminProductOptionCreateResponseDto createProductOption(UUID productId, AdminProductOptionCreateRequestDto requestDto);
    // ✅ 상품 옵션 삭제
    void deleteProductOption(UUID productId, UUID optionId);
}

