package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.application.event.producer.ProductEventProducer;
import com.example.unbox_product.product.presentation.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_product.product.presentation.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_product.product.presentation.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_product.product.presentation.mapper.AdminProductOptionMapper;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import com.example.unbox_product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.product.ProductOptionDeletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductOptionServiceImpl implements AdminProductOptionService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final AdminProductOptionMapper adminProductMapper;

    private final ProductEventProducer productEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    // ✅ 상품 옵션 목록 조회
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<AdminProductOptionListResponseDto> getProductOptions(UUID productId, Pageable pageable) {
        Page<ProductOption> options;

        if (productId != null) {
            options = productOptionRepository.findByProductIdAndDeletedAtIsNull(productId, pageable);
        } else {
            options = productOptionRepository.findAllByDeletedAtIsNull(pageable);
        }

        return options.map(adminProductMapper::toAdminProductOptionResponseDto);
    }

    // ✅ 상품 옵션 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminProductOptionCreateResponseDto createProductOption(UUID productId, AdminProductOptionCreateRequestDto requestDto) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (productOptionRepository.existsByProductAndNameAndDeletedAtIsNull(product, requestDto.getProductOptionName())) {
            throw new CustomException(ErrorCode.PRODUCT_OPTION_ALREADY_EXISTS);
        }

        ProductOption option = ProductOption.createProductOption(product, requestDto.getProductOptionName());
        ProductOption savedOption = productOptionRepository.save(option);
        return adminProductMapper.toAdminProductOptionCreateResponseDto(savedOption);
    }

    // ✅ 상품 옵션 삭제
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteProductOption(UUID productId, UUID optionId, String deletedBy) {

        // 1. [최적화] 상품 존재 여부만 가볍게 체크 (데이터 로딩 X)
        boolean productExists = productRepository.existsByIdAndDeletedAtIsNull(productId);
        if (!productExists) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 옵션 조회
        ProductOption option = productOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 3. 관계 검증 (옵션이 해당 상품의 것인지)
        if (!option.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.INVALID_PRODUCT_OPTION);
        }

        // 4. Soft Delete
        option.softDelete(deletedBy);

        // 5. [추가] 부모(상품) 캐시 삭제
        // 옵션이 변경되면 상품 정보(가격, 옵션목록)가 변하므로 상품 캐시를 지워야 합니다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 상품 상세 정보, 가격 정보 캐시 날리기
                redisTemplate.delete("product:info:" + productId);
                redisTemplate.delete("product:prices:" + productId);

                ProductOptionDeletedEvent event = new ProductOptionDeletedEvent(optionId);
                productEventProducer.publishProductOptionDeleted(event);
            }
        });


    }
}
