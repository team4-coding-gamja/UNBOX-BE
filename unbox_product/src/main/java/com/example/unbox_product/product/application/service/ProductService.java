package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.response.BrandListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForOrderInfoResponse;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForSellingBidInfoResponse;
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

    Page<ReviewListResponseDto> getReviewsByProduct(UUID productId, Pageable pageable);

    // ===========================
    // MSA 준비: 다른 서비스용 API
    // ===========================

    ProductOptionForOrderInfoResponse getProductOptionForOrder(UUID productOptionId);

    ProductOptionForSellingBidInfoResponse getProductOptionForSellingBid(UUID productOptionId);
}
