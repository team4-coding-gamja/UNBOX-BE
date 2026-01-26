package com.example.unbox_product.product.application.service;

import com.example.unbox_common.event.order.OrderCancelledEvent;
import com.example.unbox_product.product.application.event.producer.ProductEventProducer;
import com.example.unbox_product.product.presentation.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_product.product.presentation.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_product.product.presentation.dto.request.ProductSearchCondition;
import com.example.unbox_product.product.presentation.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_product.product.domain.entity.ProductOption;
import com.example.unbox_product.product.presentation.mapper.AdminProductMapper;
import com.example.unbox_product.product.domain.entity.Brand;
import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.repository.BrandRepository;
import com.example.unbox_product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.product.ProductDeletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

        private final ProductRepository productRepository;
        private final BrandRepository brandRepository;
        private final ProductOptionRepository productOptionRepository;
        private final AdminProductMapper adminProductMapper;
        private final ProductEventProducer productEventProducer;
        private final RedisTemplate<String, Object> redisTemplate;


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
                                requestDto.getProductName(),
                                requestDto.getModelNumber(),
                                requestDto.getCategory(),
                                requestDto.getProductImageUrl(),
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
                                requestDto.getProductName(),
                                requestDto.getModelNumber(),
                                requestDto.getCategory(),
                                requestDto.getProductImageUrl());

                // 캐시 삭제 (정보만)
                redisTemplate.delete("product:info:" + productId);

                return adminProductMapper.toAdminProductUpdateResponseDto(product);
        }

        @Override
        @Transactional
        @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
        public void deleteProduct(UUID productId, String deletedBy) {
                // 1. 상품 조회
                Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                // 2. 이벤트 발행을 위해 ID 목록은 필요하므로 조회
                List<UUID> deletedOptionIds = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId)
                        .stream()
                        .map(ProductOption::getId)
                        .toList();

                // 3. 상품 삭제
                product.softDelete(deletedBy);

                // 4. 옵션 일괄 삭제 (쿼리 1번 실행)
                // forEach 대신 repository 메서드 호출
                if (!deletedOptionIds.isEmpty()) {
                        productOptionRepository.deleteByProductId(productId, deletedBy);
                }

                // 5. 캐시 삭제 (커밋 후 실행)
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                redisTemplate.delete("product:info:" + productId);
                                redisTemplate.delete("product:prices:" + productId);
                        }
                });

                // 6. 상품 삭제 이벤트 발행
                ProductDeletedEvent event = new ProductDeletedEvent(
                        product.getId(),
                        deletedOptionIds
                );
                productEventProducer.publishProductDeleted(event);
        }
}
