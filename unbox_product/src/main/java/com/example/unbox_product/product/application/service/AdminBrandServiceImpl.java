package com.example.unbox_product.product.application.service;

import com.example.unbox_common.event.product.ProductDeletedEvent;
import com.example.unbox_product.product.application.event.producer.ProductEventProducer;
import com.example.unbox_product.product.presentation.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_product.product.presentation.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import com.example.unbox_product.product.presentation.mapper.AdminBrandMapper;
import com.example.unbox_product.product.domain.entity.Brand;
import com.example.unbox_product.product.domain.repository.BrandRepository;
import com.example.unbox_product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.product.BrandDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final ProductEventProducer productEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    // ✅ 브랜드 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('ROLE_MASTER', 'ROLE_MANAGER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_MASTER', 'ROLE_MANAGER')")
    public AdminBrandDetailResponseDto getBrandDetail(UUID brandId) {

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        return adminBrandMapper.toAdminBrandDetailResponseDto(brand);
    }

    // ✅ 브랜드 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_MASTER', 'ROLE_MANAGER')")
    public AdminBrandCreateResponseDto createBrand(AdminBrandCreateRequestDto requestDto) {
        if (brandRepository.existsByNameAndDeletedAtIsNull(requestDto.getBrandName())) {
            throw new CustomException(ErrorCode.BRAND_ALREADY_EXISTS);
        }

        Brand brand = Brand.createBrand(
                requestDto.getBrandName(),
                requestDto.getBrandImageUrl()
        );
        Brand savedBrand = brandRepository.save(brand);
        return adminBrandMapper.toAdminBrandCreateResponseDto(savedBrand);
    }

    // ✅ 브랜드 수정
    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_MASTER', 'ROLE_MANAGER')")
    public AdminBrandUpdateResponseDto updateBrand(UUID brandId, AdminBrandUpdateRequestDto requestDto) {

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        if (requestDto.getBrandName() != null && !requestDto.getBrandName().trim().isEmpty()) {
            String newName = requestDto.getBrandName().trim();
            if (brandRepository.existsByNameAndIdNotAndDeletedAtIsNull(newName, brandId)) {
                throw new CustomException(ErrorCode.BRAND_ALREADY_EXISTS);
            }
            brand.updateName(newName);
        }

        if (requestDto.getBrandImageUrl() != null) {
            brand.updateLogoUrl(requestDto.getBrandImageUrl());
        }

        return adminBrandMapper.toAdminBrandUpdateResponseDto(brand);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_MASTER', 'ROLE_MANAGER')")
    public void deleteBrand(UUID brandId, String deletedBy) {

        Brand brand = brandRepository.findByIdAndDeletedAtIsNull(brandId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        // 1. 삭제 대상 상품 조회 (이벤트 발행 및 옵션 삭제를 위해 ID 추출 용도)
        // 팁: 단순히 ID만 필요하다면 엔티티 전체를 조회하는 것보다 ID만 조회하는 Projection을 쓰면 더 가볍습니다.
        List<Product> products = productRepository.findAllByBrandIdAndDeletedAtIsNull(brandId);

        List<UUID> deletedProductIds = products.stream()
                .map(Product::getId)
                .toList();

        // 2. 옵션 ID 추출 (이벤트 발행 용도)
        List<UUID> deletedOptionIds = Collections.emptyList();
        if (!deletedProductIds.isEmpty()) {
            deletedOptionIds = productOptionRepository.findAllByProductIdInAndDeletedAtIsNull(deletedProductIds)
                    .stream()
                    .map(ProductOption::getId)
                    .toList();
        }

        // ==========================================
        // 3. [개선] Bulk Update로 데이터 삭제 (순서: 자식 -> 부모)
        // ==========================================

        // 3-1. 옵션 일괄 삭제 (상품이 하나라도 있을 때만 실행)
        if (!deletedProductIds.isEmpty()) {
            productOptionRepository.deleteByProductIdsIn(deletedProductIds, deletedBy);
        }

        // 3-2. 상품 일괄 삭제
        productRepository.deleteByBrandId(brandId, deletedBy);

        // 3-3. 브랜드 삭제
        brand.softDelete(deletedBy);

        // ==========================================
        // 4. [필수] 캐시 삭제 (TransactionSynchronizationManager)
        // ==========================================
        List<UUID> finalDeletedProductIds = deletedProductIds; // 람다에서 쓰기 위해 사실상 final

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 삭제된 상품들의 상세 정보 캐시도 모두 날려야 함 (루프 돌며 삭제)
                // Redis pipeline을 쓰면 더 좋지만, 개수가 많지 않다면 일단 forEach로 처리
                for (UUID prodId : finalDeletedProductIds) {
                    redisTemplate.delete("product:info:" + prodId);
                    redisTemplate.delete("product:prices:" + prodId);
                }
            }
        });

        // 5. 이벤트 발행
        BrandDeletedEvent event = new BrandDeletedEvent(
                brand.getId(),
                deletedProductIds,
                deletedOptionIds
        );
        productEventProducer.publishBrandDeleted(event);
    }
}
