package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.product.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.mapper.AdminBrandMapper;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.event.product.BrandDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBrandServiceImpl implements AdminBrandService {

    private final BrandRepository brandRepository;
    private final AdminBrandMapper adminBrandMapper;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기

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

        // 2. 삭제 대상인 상품들 조회
        // (ProductRepository에 메서드 추가 필요 - 아래 참조)
        List<Product> products = productRepository.findAllByBrandIdAndDeletedAtIsNull(brandId);

        // 3. 상품 ID 리스트 추출
        List<UUID> deletedProductIds = products.stream()
                .map(Product::getId)
                .toList();

        // 4. 옵션 데이터 조회 및 ID 추출
        // ⚠️ 수정 포인트: List를 넣으려면 메서드 명에 'In'이 들어가야 함 (findAllByProductId -> findAllByProductIdIn)
        List<ProductOption> options = Collections.emptyList();

        if (!deletedProductIds.isEmpty()) {
            options = productOptionRepository.findAllByProductIdInAndDeletedAtIsNull(deletedProductIds);
        }

        List<UUID> deletedOptionIds = options.stream()
                .map(ProductOption::getId)
                .toList();

        // 5. 하위 데이터 Soft Delete 처리 (순차적 처리)
        // (이벤트를 보내더라도, 현재 도메인의 데이터 정합성은 여기서 맞춰야 합니다)
        options.forEach(option -> option.softDelete(deletedBy)); // 옵션 삭제
        products.forEach(product -> product.softDelete(deletedBy)); // 상품 삭제
        brand.softDelete(deletedBy); // 브랜드 삭제

        // 6. 이벤트 발행
        // (외부 도메인은 이 ID 리스트를 보고 자기네 데이터를 정리함)
        if (!deletedProductIds.isEmpty() || !deletedOptionIds.isEmpty()) {
            eventPublisher.publishEvent(new BrandDeletedEvent(brandId, deletedProductIds, deletedOptionIds));
        }
    }
}
