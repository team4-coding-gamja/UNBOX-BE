package com.example.unbox_be.domain.admin.product.service;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
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
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final AdminRepository adminRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final BrandRepository brandRepository;

    // ✅ 상품 등록
    @Override
    public AdminProductCreateResponseDto createProduct(String email, AdminProductCreateRequestDto requestDto) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
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
        return AdminProductMapper.toAdminProductCreateResponseDto(savedProduct);
    }

    // ✅ 상품 삭제
    @Override
    public void deleteProduct(String email, UUID productId) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        // 실무에서는 소프트 삭제 추천 (현재는 하드 삭제)
        productRepository.delete(product);
    }

    // ✅ 상품 옵션 등록
    @Override
    public AdminProductOptionCreateResponseDto createProductOption(String email, UUID productId, AdminProductOptionCreateRequestDto requestDto) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        if (productOptionRepository.existsByProductAndOption(product, requestDto.getOption())) {
            throw new CustomException(ErrorCode.PRODUCT_OPTION_ALREADY_EXISTS);
        }

        ProductOption option = ProductOption.createProductOption(product, requestDto.getOption());
        ProductOption savedOption = productOptionRepository.save(option);
        return AdminProductMapper.toAdminProductOptionCreateResponseDto(savedOption);
    }

    // ✅ 상품 옵션 삭제
    @Override
    public void deleteProductOption(String email, UUID productId, UUID optionId) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));
        productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 옵션이 해당 상품 소속인지 검증
        if (!option.getProduct().getId().equals(productId)) {
            throw new CustomException(ErrorCode.INVALID_PRODUCT_OPTION);
        }

        productOptionRepository.delete(option);
    }
}
