package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.response.BrandListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_product.product.domain.entity.Brand;
import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.entity.ProductOption;
import com.example.unbox_product.product.presentation.mapper.BrandMapper;
import com.example.unbox_product.product.presentation.mapper.ProductClientMapper;
import com.example.unbox_product.product.presentation.mapper.ProductMapper;
import com.example.unbox_product.product.domain.repository.BrandRepository;
import com.example.unbox_product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.reviews.entity.Review;
import com.example.unbox_product.reviews.mapper.ReviewMapper;
import com.example.unbox_product.reviews.repository.ReviewRepository;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForOrderInfoResponse;
import com.example.unbox_product.product.presentation.dto.internal.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final ProductOptionRepository productOptionRepository;
        private final BrandRepository brandRepository;
        private final ProductMapper productMapper;
        private final BrandMapper brandMapper;
        private final ReviewRepository reviewRepository;
        private final ReviewMapper reviewMapper;
        private final ProductClientMapper productClientMapper;

        // ✅ 상품 목록 조회 (검색 + 페이징) - 최저가 조회 제거 버전
        public Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable) {

                // 1️⃣ category 문자열을 Category Enum으로 변환
                Category categoryEnum = Category.fromNullable(category);

                // 2️⃣ 브랜드 / 카테고리 / 키워드 조건으로 상품을 페이징 조회 (deletedAt IS NULL 포함)
                Page<Product> products = productRepository.findByFiltersAndDeletedAtIsNull(
                        brandId,
                        categoryEnum,
                        keyword,
                        pageable
                );

                // 3️⃣ 최저가 조회 로직 제거
                return products.map(productMapper::toProductListResponseDto);
        }

        // ✅ 상품 상세 조회 (최저가 완전 제거)
        public ProductDetailResponseDto getProductDetail(UUID productId) {
                Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                return productMapper.toProductDetailDto(product);
        }

        // ✅ 상품 옵션 조회 (옵션별 최저가 완전 제거)
        @Override
        public List<ProductOptionListResponseDto> getProductOptions(UUID productId) {
                if (!productRepository.existsById(productId)) {
                        throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                List<ProductOption> options =
                        productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);

                return options.stream()
                        .map(productMapper::toProductOptionListDto)
                        .toList();
        }

        // ✅ 브랜드 전체 조회
        @Override
        public List<BrandListResponseDto> getAllBrands() {
                List<Brand> brands = brandRepository.findAll();
                return brands.stream()
                                .map(brandMapper::toBrandListDto)
                                .toList();
        }

        @Transactional
        public void addReviewData(UUID productId, int score) {
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                product.addReviewData(score);
        }

        @Transactional
        public void deleteReviewData(UUID productId, int score) {
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                product.deleteReviewData(score);
        }

        @Transactional
        public void updateReviewData(UUID productId, int oldScore, int newScore) {
                if (oldScore == newScore)
                        return;

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                product.updateReviewData(oldScore, newScore);
        }

        // ✅ 상품별 리뷰 조회
        @Override
        @Transactional(readOnly = true)
        public Page<ReviewListResponseDto> getReviewsByProduct(UUID productId, Pageable pageable) {
                // 상품 존재 여부 확인
                if (!productRepository.existsByIdAndDeletedAtIsNull(productId)) {
                        throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                // 리뷰 조회 및 DTO 변환
                Page<Review> reviews = reviewRepository.findAllByProductSnapshotProductIdAndDeletedAtIsNull(productId, pageable);
                return reviews.map(reviewMapper::toReviewListResponseDto);
        }

        // ===========================
        // MSA 준비: 다른 서비스용 API
        // ===========================

        // ✅ 상품 옵션 조회 (주문용)
        @Override
        @Transactional(readOnly = true)
        public ProductOptionForOrderInfoResponse getProductOptionForOrder(UUID productOptionId) {
                ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(productOptionId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

                return productClientMapper.toProductOptionForOrderInfoResponse(productOption);
        }

        // ✅ 상품 옵션 조회 (판매용)
        @Override
        @Transactional(readOnly = true)
        public ProductOptionForSellingBidInfoResponse getProductOptionForSellingBid(UUID optionId) {

                ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                return productClientMapper.toProductOptionForSellingBidInfoResponse(productOption);
        }
}
