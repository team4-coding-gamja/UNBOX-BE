package com.example.unbox_be.domain.admin.brand.service;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.mapper.AdminBrandMapper;
import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBrandServiceImpl implements AdminBrandService {

    private final BrandRepository brandRepository;

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
        return AdminBrandMapper.toAdminBrandCreateResponseDto(savedBrand);
    }

    // ✅ 브랜드 삭제
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteBrand(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        brandRepository.delete(brand); // 실무에서는 hard delete보단 soft delete 추천
    }
}
