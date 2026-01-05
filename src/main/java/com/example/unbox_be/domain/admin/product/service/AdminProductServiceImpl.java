package com.example.unbox_be.domain.admin.product.service;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.admin.product.mapper.AdminProductMapper;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final BrandRepository brandRepository;
    private final AdminProductMapper adminProductMapper;

    // ✅ 상품 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminProductCreateResponseDto createProduct(AdminProductCreateRequestDto requestDto) {
        Brand brand = brandRepository.findById(requestDto.getBrandId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRAND_NOT_FOUND));

        Product product = Product.createProduct(
                requestDto.getName(),
                requestDto.getModelNumber(),
                requestDto.getCategory(),
                requestDto.getImageUrl(),
                brand
        );
        Product savedProduct = productRepository.save(product);
        return adminProductMapper.toAdminProductCreateResponseDto(savedProduct);
    }

    // ✅ 상품 수정
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminProductUpdateResponseDto updateProduct(UUID productId, AdminProductUpdateRequestDto requestDto) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        if (requestDto.getModelNumber() != null &&
                productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull(requestDto.getModelNumber(), productId)) {
            throw new CustomException(ErrorCode.PRODUCT_MODEL_NUMBER_ALREADY_EXISTS);
        }

        product.update(
                requestDto.getName(),
                requestDto.getModelNumber(),
                requestDto.getCategory(),
                requestDto.getImageUrl()
        );

        return adminProductMapper.toAdminProductUpdateResponseDto(product);
    }


    // ✅ 상품 삭제
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public void deleteProduct(UUID productId, String deletedBy) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.softDelete(deletedBy);
    }

    // ✅ 상품 옵션 등록
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public AdminProductOptionCreateResponseDto createProductOption(UUID productId, AdminProductOptionCreateRequestDto requestDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (productOptionRepository.existsByProductAndOption(product, requestDto.getOption())) {
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
        productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        if (!option.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.INVALID_PRODUCT_OPTION);
        }

        option.softDelete(deletedBy);
    }
}
