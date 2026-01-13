package com.example.unbox_be.domain.product.implementation;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.global.client.product.ProductClient;
import com.example.unbox_be.global.client.product.dto.ProductOptionForOrderInfoResponse;
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

    // ✅ 상품 옵션 조회 (주문용)
    @Override
    public ProductOptionForOrderInfoResponse getProductForOrder(UUID productOptionId) {
        ProductOption productOption = productOptionRepository.findById(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        return ProductOptionForOrderInfoResponse.from(productOption);
    }
}
