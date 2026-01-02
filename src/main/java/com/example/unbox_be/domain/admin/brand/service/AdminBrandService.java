package com.example.unbox_be.domain.admin.brand.service;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;

import java.util.UUID;

public interface AdminBrandService {

    // ✅ 브랜드 등록
    AdminBrandCreateResponseDto createBrand(String email, AdminBrandCreateRequestDto requestDto);
    // ✅ 브랜드 삭제
    void deleteBrand(String email, UUID brandId);
}
