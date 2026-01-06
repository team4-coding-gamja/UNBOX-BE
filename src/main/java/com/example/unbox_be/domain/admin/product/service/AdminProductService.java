package com.example.unbox_be.domain.admin.product.service;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminProductService {

    // ✅ 상품 목록 조회 (검색 + 페이징)
    Page<AdminProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable);
    // ✅ 상품 등록
    AdminProductCreateResponseDto createProduct(AdminProductCreateRequestDto requestDto);
    // ✅ 상품 수정
    AdminProductUpdateResponseDto updateProduct(UUID productId, AdminProductUpdateRequestDto requestDto);
    // ✅ 상품 삭제
    void deleteProduct(UUID productId, String deletedBy);
}
