package com.example.unbox_product.product.application.service;

// import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDtoV2;
import com.example.unbox_product.product.presentation.dto.response.SliceResponse;
import java.util.UUID;

public interface TestProductService {
    SliceResponse<ProductListResponseDtoV2> getProductsV2(Long lastScore, UUID lastId, UUID brandId, int size);
}