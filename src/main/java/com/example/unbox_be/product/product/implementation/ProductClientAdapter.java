package com.example.unbox_be.product.product.implementation;

import com.example.unbox_be.product.product.entity.ProductOption;
import com.example.unbox_be.product.product.repository.ProductOptionRepository;
import com.example.unbox_be.common.client.product.ProductClient;
import com.example.unbox_be.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
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
        ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        return ProductOptionForOrderInfoResponse.from(productOption);
    }

    // ✅ 상품 옵션 조회 (판매용)
    public ProductOptionForSellingBidInfoResponse getProductOptionForSellingBid(UUID productOptionId) {
        ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        return ProductOptionForSellingBidInfoResponse.from(productOption);
    }
}
