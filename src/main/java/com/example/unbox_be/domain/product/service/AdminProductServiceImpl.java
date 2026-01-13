package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.request.ProductSearchCondition;
import com.example.unbox_be.domain.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.product.mapper.AdminProductMapper;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

        private final ProductRepository productRepository;
        private final BrandRepository brandRepository;
        private final AdminProductMapper adminProductMapper;

        // ✅ 상품 목록 조회
        @Override
        @Transactional(readOnly = true)
        @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
        public Page<AdminProductListResponseDto> getProducts(ProductSearchCondition condition, Pageable pageable) {

                UUID brandId = condition.getBrandId();
                String category = condition.getCategory();
                String keyword = condition.getKeyword();

                Category categoryEnum = Category.fromNullable(category);

                Page<Product> products = productRepository.findByFiltersAndDeletedAtIsNull(brandId, categoryEnum,
                                keyword, pageable);

                return products.map(adminProductMapper::toAdminProductListResponseDto);
        }

        // ✅ 상품 등록
        @Override
        @Transactional
        @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
        public AdminProductCreateResponseDto createProduct(AdminProductCreateRequestDto requestDto) {
                Brand brand = brandRepository.findByIdAndDeletedAtIsNull(requestDto.getBrandId())
                                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

                Product product = Product.createProduct(
                                requestDto.getName(),
                                requestDto.getModelNumber(),
                                requestDto.getCategory(),
                                requestDto.getImageUrl(),
                                brand);
                Product savedProduct = productRepository.save(product);
                return adminProductMapper.toAdminProductCreateResponseDto(savedProduct);
        }

        // ✅ 상품 수정
        @Override
        @Transactional
        @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
        public AdminProductUpdateResponseDto updateProduct(UUID productId, AdminProductUpdateRequestDto requestDto) {

                Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                if (requestDto.getModelNumber() != null &&
                                productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull(
                                                requestDto.getModelNumber(), productId)) {
                        throw new CustomException(ErrorCode.PRODUCT_MODEL_NUMBER_ALREADY_EXISTS);
                }

                product.update(
                                requestDto.getName(),
                                requestDto.getModelNumber(),
                                requestDto.getCategory(),
                                requestDto.getImageUrl());

                return adminProductMapper.toAdminProductUpdateResponseDto(product);
        }

        // ✅ 상품 삭제
        @Override
        @Transactional
        @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
        public void deleteProduct(UUID productId, String deletedBy) {
                Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                product.softDelete(deletedBy);
        }
}
