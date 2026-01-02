package com.example.unbox_be.domain.admin.service;

import com.example.unbox_be.domain.admin.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.entity.Admin;
import com.example.unbox_be.domain.admin.mapper.AdminMapper;
import com.example.unbox_be.domain.admin.repository.AdminRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBrandServiceImpl implements AdminBrandService {

    private final AdminRepository adminRepository;
    private final BrandRepository brandRepository;


    // 브랜드 등록
    public AdminBrandCreateResponseDto createBrand(String email, AdminBrandCreateRequestDto adminBrandCreateRequestDto) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        if (brandRepository.existsByName(adminBrandCreateRequestDto.getName())) {
            throw new CustomException(ErrorCode.BRAND_ALREADY_EXISTS);
        }
        Brand brand = Brand.createBrand(
                adminBrandCreateRequestDto.getName(),
                adminBrandCreateRequestDto.getLogoUrl()
        );
        Brand savedBrand = brandRepository.save(brand);
        return AdminMapper.toAdminBrandCreateResponseDto(savedBrand);
    }

    // 브랜드 삭제
    public void deleteBrand(String email, UUID brandId) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        brandRepository.delete(brand); // 실무에서는 hard delete보단 soft delete 추천
    }
}
