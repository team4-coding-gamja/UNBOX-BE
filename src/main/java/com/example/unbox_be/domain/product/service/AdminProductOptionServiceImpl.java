package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_be.domain.product.mapper.AdminProductOptionMapper;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.global.event.product.ProductOptionDeletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductOptionServiceImpl implements AdminProductOptionService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final AdminProductOptionMapper adminProductMapper;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기

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
        if (productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, requestDto.getOption())) {
            throw new CustomException(ErrorCode.PRODUCT_OPTION_ALREADY_EXISTS);
        }

        ProductOption option = ProductOption.createProductOption(product, requestDto.getOption());
        ProductOption savedOption = productOptionRepository.save(option);
        return adminProductMapper.toAdminProductOptionCreateResponseDto(savedOption);
    }

    // ✅ 상품 옵션 삭제
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteProductOption(UUID productId, UUID optionId, String deletedBy) {
        productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductOption option = productOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        if (!option.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.INVALID_PRODUCT_OPTION);
        }

        option.softDelete(deletedBy);

        eventPublisher.publishEvent(new ProductOptionDeletedEvent(optionId));

    }
}
