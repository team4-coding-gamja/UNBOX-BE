package com.example.unbox_be.global.client.product;

import com.example.unbox_be.global.client.product.dto.ProductOptionInfoResponse;
import java.util.UUID;

public interface ProductClient {
    // 상품 옵션 존재 여부 확인 및 정보 조회
    ProductOptionInfoResponse getProductOption(UUID productOptionId);
}
