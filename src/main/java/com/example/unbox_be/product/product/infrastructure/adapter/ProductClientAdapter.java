package com.example.unbox_be.product.product.infrastructure.adapter;

import com.example.unbox_be.product.product.domain.entity.ProductOption;
import com.example.unbox_be.product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_be.common.client.product.ProductClient;
import com.example.unbox_be.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import com.example.unbox_be.product.product.presentation.mapper.ProductClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductClientAdapter implements ProductClient {

    private final ProductOptionRepository productOptionRepository;
    private final ProductClientMapper productClientMapper;

    // ✅ 상품 옵션 조회 (주문용)
    @Override
    public ProductOptionForOrderInfoResponse getProductForOrder(UUID productOptionId) {
        ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        return productClientMapper.toProductOptionForOrderInfoResponse(productOption);
    }

    // ✅ 상품 옵션 조회 (판매용)
    public ProductOptionForSellingBidInfoResponse getProductOptionForSellingBid(UUID productOptionId) {
        ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(productOptionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));
        return productClientMapper.toProductOptionForSellingBidInfoResponse(productOption);
    }
}
