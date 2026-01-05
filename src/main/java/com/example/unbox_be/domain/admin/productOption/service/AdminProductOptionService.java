package com.example.unbox_be.domain.admin.productOption.service;

import com.example.unbox_be.domain.admin.productOption.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.productOption.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.productOption.dto.response.AdminProductOptionListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminProductOptionService {

    // ✅ 상품 옵션 목록 조회
    Page<AdminProductOptionListResponseDto> getProductOptions(UUID productId, Pageable pageable);
    // ✅ 상품 옵션 등록
    AdminProductOptionCreateResponseDto createProductOption(UUID productId, AdminProductOptionCreateRequestDto requestDto);
    // ✅ 상품 옵션 삭제
    void deleteProductOption(UUID productId, UUID optionId, String deletedBy);
}
