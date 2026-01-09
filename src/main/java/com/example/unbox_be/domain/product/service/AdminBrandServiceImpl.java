package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.product.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.product.mapper.AdminBrandMapper;
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
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<AdminBrandListResponseDto> getBrands(String keyword, Pageable pageable) {

        Page<Brand> page;

        if (keyword == null || keyword.trim().isEmpty()) {
            page = brandRepository.findAllByDeletedAtIsNull(pageable);
        } else {
            page = brandRepository.searchByNameAndDeletedAtIsNull(keyword.trim(), pageable);
        }

        return page.map(adminBrandMapper::toAdminBrandListResponseDto);
    }

    // ✅ 브랜드 상세 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminBrandDetailResponseDto getBrandDetail(UUID brandId) {

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        return adminBrandMapper.toAdminBrandDetailResponseDto(brand);
    }

    // ✅ 브랜드 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminBrandCreateResponseDto createBrand(AdminBrandCreateRequestDto requestDto) {
        if (brandRepository.existsByNameAndDeletedAtIsNull(requestDto.getName())) {
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

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()) {
            String newName = requestDto.getName().trim();
            if (brandRepository.existsByNameAndIdNotAndDeletedAtIsNull(newName, brandId)) {
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
        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        brand.softDelete(deletedBy);
    }
}
