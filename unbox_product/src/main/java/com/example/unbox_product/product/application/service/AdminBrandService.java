package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_product.product.presentation.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandUpdateResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminBrandService {

    // ✅ 브랜드 목록 조회
    Page<AdminBrandListResponseDto> getBrands(String keyword, Pageable pageable);
    // ✅ 브랜드 조회
    AdminBrandDetailResponseDto getBrandDetail(UUID brandId);
    // ✅ 브랜드 등록
    AdminBrandCreateResponseDto createBrand(AdminBrandCreateRequestDto requestDto);
    // ✅ 브랜드 수정
    AdminBrandUpdateResponseDto updateBrand(UUID brandId, AdminBrandUpdateRequestDto requestDto);
    // ✅ 브랜드 삭제
    void deleteBrand(UUID brandId, String deletedBy);

}
