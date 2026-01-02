package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminBrandCreateResponseDto;

import java.util.UUID;

public interface AdminBrandService {
    // 브랜드 등록 API
    AdminBrandCreateResponseDto createBrand(String email, AdminBrandCreateRequestDto requestDto);

    // 브랜드 삭제
    void deleteBrand(String email, UUID brandId);
}
