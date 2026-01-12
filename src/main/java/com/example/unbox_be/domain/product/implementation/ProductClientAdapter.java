package com.example.unbox_be.domain.product.implementation;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.global.client.product.ProductClient;
import com.example.unbox_be.global.client.product.dto.ProductOptionInfoResponse;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductClientAdapter implements ProductClient {

    private final ProductOptionRepository productOptionRepository;

    @Override
    public ProductOptionInfoResponse getProductOption(UUID productOptionId) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        Product product = productOption.getProduct();

        return ProductOptionInfoResponse.builder()
                .productOptionId(productOption.getId())
                .productId(product.getId())
                .productName(product.getName())
                .modelNumber(product.getModelNumber())
                .optionName(productOption.getOption())
                .imageUrl(product.getImageUrl())
                .brandId(product.getBrand().getId())
                .brandName(product.getBrand().getName())
                .build();
    }
}
