package com.example.unbox_be.domain.admin.brand.service;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminBrandService {

    // ✅ 브랜드 조회
    Page<AdminBrandListResponseDto> getBrands(String keyword, Pageable pageable);
    // ✅ 브랜드 등록
    AdminBrandCreateResponseDto createBrand(AdminBrandCreateRequestDto requestDto);
    // ✅ 브랜드 삭제
    void deleteBrand(UUID brandId, String deletedBy);

}
