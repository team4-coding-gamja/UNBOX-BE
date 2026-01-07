package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.response.BrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductOptionListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable);

    ProductDetailResponseDto getProductDetail(UUID productId);

    List<ProductOptionListResponseDto> getProductOptions(UUID productId);

    List<BrandListResponseDto> getAllBrands();

    void addReviewData(UUID productId, int score);

    void deleteReviewData(UUID productId, int score);

    void updateReviewData(UUID productId, int oldScore, int newScore);
}
