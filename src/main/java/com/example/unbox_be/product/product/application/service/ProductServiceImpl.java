package com.example.unbox_be.product.product.application.service;

import com.example.unbox_be.product.product.presentation.dto.response.BrandListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.product.product.domain.entity.Brand;
import com.example.unbox_be.product.product.domain.entity.Category;
import com.example.unbox_be.product.product.domain.entity.Product;
import com.example.unbox_be.product.product.domain.entity.ProductOption;
import com.example.unbox_be.product.product.presentation.mapper.BrandMapper;
import com.example.unbox_be.product.product.presentation.mapper.ProductClientMapper;
import com.example.unbox_be.product.product.presentation.mapper.ProductMapper;
import com.example.unbox_be.product.product.domain.repository.BrandRepository;
import com.example.unbox_be.product.product.domain.repository.ProductOptionRepository;
import com.example.unbox_be.product.product.domain.repository.ProductRepository;
import com.example.unbox_be.product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_be.product.reviews.entity.Review;
import com.example.unbox_be.product.reviews.mapper.ReviewMapper;
import com.example.unbox_be.product.reviews.repository.ReviewRepository;
import com.example.unbox_be.trade.domain.entity.SellingStatus;
import com.example.unbox_be.trade.domain.repository.SellingBidRepository;
import com.example.unbox_be.common.client.product.dto.ProductOptionForOrderInfoResponse;
import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductServiceImpl implements ProductService {

        private final ProductRepository productRepository;
        private final ProductOptionRepository productOptionRepository;
        private final BrandRepository brandRepository;
        private final ProductMapper productMapper;
        private final BrandMapper brandMapper;
        private final SellingBidRepository sellingBidRepository;
        private final ReviewRepository reviewRepository;
        private final ReviewMapper reviewMapper;
        private final ProductClientMapper productClientMapper;

        // ✅ 상품 목록 조회 (검색 + 페이징)
        public Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable) {

                // 1️⃣ category 문자열을 Category Enum으로 변환
                //    - null 또는 빈 문자열이면 필터를 적용하지 않기 위함
                Category categoryEnum = Category.fromNullable(category);

                // 2️⃣ 브랜드 / 카테고리 / 키워드 조건으로 상품을 페이징 조회
                //    - deletedAt IS NULL 조건 포함
                Page<Product> products = productRepository.findByFiltersAndDeletedAtIsNull(
                        brandId,
                        categoryEnum,
                        keyword,
                        pageable
                );

                // 3️⃣ 현재 페이지에 포함된 상품 ID만 추출
                //    - 이후 최저가 조회 시 전체 상품을 다시 조회하지 않기 위함 (성능 최적화)
                List<UUID> productIds = products.getContent().stream()
                        .map(Product::getId)
                        .toList();

                // 4️⃣ 조회된 상품이 하나도 없으면
                //    - DB 추가 조회 없이 바로 빈 가격(0)으로 응답
                if (productIds.isEmpty()) {
                        return products.map(p -> productMapper.toProductListResponseDto(p, 0));
                }

                // 5️⃣ 조회된 상품들의 모든 옵션을 한 번에 조회
                //    - 상품 → 옵션 조회를 개별로 하면 N+1 문제가 발생함
                List<ProductOption> allOptions = productOptionRepository
                        .findAllByProductIdInAndDeletedAtIsNull(productIds);

                // 6️⃣ 옵션 ID만 추출
                //    - 판매 입찰(SellingBid) 최저가 조회에 사용
                List<UUID> optionIds = allOptions.stream()
                        .map(ProductOption::getId)
                        .toList();

                // 7️⃣ 옵션 ID 기준으로 최저가 조회
                //    - optionId, lowestPrice 형태의 결과를 Map으로 변환
                //    - 가격이 없으면 0으로 처리
                Map<UUID, Integer> optionPriceMap = optionIds.isEmpty()
                        ? Map.of() // 옵션이 없으면 조회하지 않음
                        : sellingBidRepository.findLowestPricesByProductOptionIds(optionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (UUID) row[0],                     // optionId
                                row -> row[1] == null ? 0                 // 가격이 없으면 0
                                        : ((Number) row[1]).intValue()     // Number → int 변환
                        ));

                // 8️⃣ 옵션별 가격을 상품별 최저가로 변환
                //    - 한 상품은 여러 옵션을 가질 수 있으므로 최소값만 선택
                Map<UUID, Integer> productPriceMap = allOptions.stream()
                        .collect(Collectors.groupingBy(
                                opt -> opt.getProduct().getId(),                  // 상품 ID 기준 그룹화
                                Collectors.mapping(
                                        opt -> optionPriceMap.getOrDefault(
                                                opt.getId(), Integer.MAX_VALUE),  // 옵션 가격
                                        Collectors.minBy(Integer::compareTo)      // 상품 내 최저가 선택
                                )
                        ))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,                                // productId
                                e -> e.getValue().orElse(0) == Integer.MAX_VALUE
                                        ? 0                                       // 가격이 없으면 0
                                        : e.getValue().orElse(0)
                        ));

                // 9️⃣ 상품 + 상품별 최저가를 DTO로 변환하여 최종 응답
                return products.map(p ->
                        productMapper.toProductListResponseDto(
                                p,
                                productPriceMap.getOrDefault(p.getId(), 0)
                        )
                );
        }

        // ✅ 상품 상세 조회
        public ProductDetailResponseDto getProductDetail(UUID productId) {
                Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                // Get all options for this product, then query lowest prices
                List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);
                List<UUID> optionIds = options.stream().map(ProductOption::getId).toList();

                Integer lowestPrice = 0;
                if (!optionIds.isEmpty()) {
                        lowestPrice = sellingBidRepository.findLowestPriceByProductOptionIds(optionIds,
                                        SellingStatus.LIVE);
                }

                // 조회된 최저가가 없으면 0(또는 null) 처리
                return productMapper.toProductDetailDto(product, lowestPrice != null ? lowestPrice : 0);
        }

        // ✅ 상품 옵션별 최저가 조회
        @Override
        public List<ProductOptionListResponseDto> getProductOptions(UUID productId) {
                if (!productRepository.existsById(productId)) {
                        throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
                }

                List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);
                List<UUID> optionIds = options.stream().map(ProductOption::getId).toList();

                Map<UUID, Integer> lowestPriceMap = sellingBidRepository.findLowestPriceByOptionIds(optionIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                row -> (UUID) row[0],
                                                row -> ((Number) row[1]).intValue()));

                return options.stream()
                                .map(option -> productMapper.toProductOptionListDto(
                                                option,
                                                lowestPriceMap.getOrDefault(option.getId(), 0)))
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
