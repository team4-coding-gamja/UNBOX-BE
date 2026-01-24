package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.redis.ProductRedisDto;
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
import com.example.unbox_product.common.client.trade.TradeClient;
import com.example.unbox_product.common.client.trade.dto.LowestPriceResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

@Slf4j
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
        private final RedisTemplate<String, Object> redisTemplate; // redis
        private final TradeClient tradeClient; // Feign Client

        // ✅ 상품 목록 조회 (검색 + 페이징) - 최저가 조회 제거 버전
        public Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword,
                        Pageable pageable) {

                // 1️⃣ category 문자열을 Category Enum으로 변환
                Category categoryEnum = Category.fromNullable(category);

                // 2️⃣ 브랜드 / 카테고리 / 키워드 조건으로 상품을 페이징 조회 (deletedAt IS NULL 포함)
                Page<Product> products = productRepository.findByFiltersAndDeletedAtIsNull(
                                brandId,
                                categoryEnum,
                                keyword,
                                pageable);

                // 3️⃣ 최저가 조회 로직 제거
                return products.map(productMapper::toProductListResponseDto);
        }

        // 상품 상세 조회 (Redis 적용)
        @Override
        @Transactional(readOnly = true)
        public ProductDetailResponseDto getProductDetail(UUID productId) {
                String infoKey = "prod:info:" + productId;
                String priceKey = "prod:prices:" + productId;

                // 1️⃣ [Redis] 상품 정보(Info) 조회 (먼저 찔러보기)
                ProductRedisDto infoDto = (ProductRedisDto) redisTemplate.opsForValue().get(infoKey);

                // 2️⃣ [Cache Miss] Redis에 정보가 없으면 DB로 간다
                if (infoDto == null) {

                        // A. 상품+브랜드 조회 (기존에 있던 메서드 활용)
                        Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

                        // B. 옵션 별도 조회 (엔티티에 필드가 없으므로 리포지토리로 따로 조회)
                        List<ProductOption> options = productOptionRepository
                                        .findAllByProductIdAndDeletedAtIsNull(productId);

                        // C. DTO 변환 (상품과 옵션 리스트를 합침)
                        infoDto = ProductRedisDto.from(product, options);

                        // D. 다음을 위해 Redis에 저장 (24시간)
                        redisTemplate.opsForValue().set(infoKey, infoDto, Duration.ofHours(24));
                }

                // 3️⃣ [Redis] 가격 조회 (가격은 항상 Redis에서 Hash 구조로 조회)
                Map<Object, Object> prices = redisTemplate.opsForHash().entries(priceKey);

                // 최저가 계산
                BigDecimal lowestPrice = prices.values().stream()
                                .map(v -> {
                                    try {
                                        return new BigDecimal(String.valueOf(v));
                                    } catch (NumberFormatException e) {
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);

                // 4️⃣ 결과 반환
                return productMapper.toProductDetailResponseDto(infoDto, lowestPrice);
        }

        // ✅ 상품 옵션 조회 (옵션별 최저가 포함) - Batch Optimization Applied
    @Override
    public List<ProductOptionListResponseDto> getProductOptions(UUID productId) {
        String infoKey = "prod:info:" + productId;
        String priceKey = "prod:prices:" + productId;

        // 1️⃣ [Redis] 가격 정보 조회 (항상 Redis에서 가져옴)
        Map<Object, Object> prices = redisTemplate.opsForHash().entries(priceKey);

        // 2️⃣ [Redis] 상품 정보 조회
        ProductRedisDto infoDto = (ProductRedisDto) redisTemplate.opsForValue().get(infoKey);

        if (infoDto != null) {
            // ✅ [Cache Hit] Redis에 있으면 바로 변환해서 반환
            List<UUID> optionIds = infoDto.getOptions().stream().map(ProductRedisDto.ProductOptionDto::getOptionId).toList();
            
            fillMissingPrices(prices, optionIds, productId); // Batch Lookup

            return infoDto.getOptions().stream()
                    .map(option -> {
                        BigDecimal price = getPriceFromMap(prices, option.getOptionId());
                        return productMapper.toProductOptionListDtoFromRedis(option, price);
                    })
                    .toList();
        }

        // 3️⃣ [Cache Miss] 없으면 DB 조회
        if (!productRepository.existsById(productId)) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);
        
        List<UUID> optionIds = options.stream().map(ProductOption::getId).toList();
        fillMissingPrices(prices, optionIds, productId); // Batch Lookup

        return options.stream()
                .map(option -> {
                    BigDecimal price = getPriceFromMap(prices, option.getId());
                    return productMapper.toProductOptionListDto(option, price);
                })
                .toList();
    }

    private void fillMissingPrices(Map<Object, Object> prices, List<UUID> optionIds, UUID productId) {
        List<UUID> missingIds = optionIds.stream()
                .filter(id -> !prices.containsKey(id.toString()))
                .toList();

        if (missingIds.isEmpty()) return;

        try {
            List<LowestPriceResponseDto> fetched = tradeClient.getLowestPrices(missingIds);

            Map<String, String> newPrices = new java.util.HashMap<>();

            for (LowestPriceResponseDto dto : fetched) {
                if (dto.getLowestPrice() != null) {
                    String val = dto.getLowestPrice().toString();
                    prices.put(dto.getProductOptionId().toString(), val); // Update local map
                    newPrices.put(dto.getProductOptionId().toString(), val);
                }
            }

            // Redis Multi-set (Cache-aside)
            if (!newPrices.isEmpty()) {
                String priceKey = "prod:prices:" + productId;
                redisTemplate.opsForHash().putAll(priceKey, newPrices);
                redisTemplate.expire(priceKey, Duration.ofMinutes(30));
                log.info("Batch updated prices for product {}, count: {}", productId, newPrices.size());
            }

        } catch (Exception e) {
            log.warn("Trade batch price lookup failed for product {}", productId, e);
            // Fallback: missing prices remain 'null' in map, handled as ZERO in getPriceFromMap
        }
    }

    private BigDecimal getPriceFromMap(Map<Object, Object> prices, UUID optionId) {
        Object val = prices.get(optionId.toString());
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
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
                Page<Review> reviews = reviewRepository.findAllByProductSnapshotProductIdAndDeletedAtIsNull(productId,
                                pageable);
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
