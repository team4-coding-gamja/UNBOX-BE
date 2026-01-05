package com.example.unbox_be.domain.admin.brand.service;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.admin.brand.mapper.AdminBrandMapper;
import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBrandServiceImpl implements AdminBrandService {

    private final BrandRepository brandRepository;
    private final AdminBrandMapper adminBrandMapper;

    // ✅ 브랜드 목록 조회
    @Override
    @Transactional(readOnly = true)
    public Page<AdminBrandListResponseDto> getBrands(String keyword, Pageable pageable) {

        Page<Brand> page;

        if (keyword == null || keyword.trim().isEmpty()) {
            page = brandRepository.findAll(pageable);
        } else {
            page = brandRepository.searchByName(keyword.trim(), pageable);
        }

        return page.map(adminBrandMapper::toAdminBrandListResponseDto);
    }

    // ✅ 브랜드 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminBrandCreateResponseDto createBrand(AdminBrandCreateRequestDto requestDto) {
        if (brandRepository.existsByName(requestDto.getName())) {
            throw new CustomException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        Brand brand = Brand.createBrand(
                requestDto.getName(),
                requestDto.getLogoUrl()
        );
        Brand savedBrand = brandRepository.save(brand);
        return adminBrandMapper.toAdminBrandCreateResponseDto(savedBrand);
    }

    // ✅ 브랜드 수정
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminBrandUpdateResponseDto updateBrand(UUID brandId, AdminBrandUpdateRequestDto requestDto) {

        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        if (brand.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.BRAND_NOT_FOUND);
        }

        if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()) {
            String newName = requestDto.getName().trim();
            if (brandRepository.existsByNameAndIdNot(newName, brandId)) {
                throw new CustomException(ErrorCode.BRAND_ALREADY_EXISTS);
            }
            brand.updateName(newName);
        }

        if (requestDto.getLogoUrl() != null) {
            brand.updateLogoUrl(requestDto.getLogoUrl());
        }

        return adminBrandMapper.toAdminBrandUpdateResponseDto(brand);
    }

    // ✅ 브랜드 삭제
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteBrand(UUID brandId, String deletedBy) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        brand.softDelete(deletedBy);
    }
}
