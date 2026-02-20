package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.SliceResponse;
import java.util.UUID;

public interface TestProductService {
    SliceResponse<ProductListResponseDto> getProductsV2(UUID lastProductId, UUID brandId, String category, String keyword, int size);
}