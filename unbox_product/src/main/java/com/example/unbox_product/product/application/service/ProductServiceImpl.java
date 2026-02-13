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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
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
@RequiredArgsConstructor
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

    // ✅ 캐시 사용 여부 플래그 (기본값: true)
    // application.yml의 app.cache.enabled 값을 읽어옴
    @Value("${app.cache.enabled:true}")
    private boolean isCacheEnabled;

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

    // ✅ 상품 상세 조회 (테스트용 분기 처리 적용)
    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponseDto getProductDetail(UUID productId) {

        productRepository.incrementPopularityScore(productId);
        redisTemplate.opsForZSet().incrementScore("products:popular:all", productId.toString(), 1);

        // 🔴 [TEST MODE] 캐시가 꺼져있으면 DB/Feign 직접 조회 로직으로 이동
        if (!isCacheEnabled) {
            return getProductDetailNoCache(productId);
        }

        // 🟢 [NORMAL MODE] 기존 Redis 캐싱 로직
        String infoKey = "product:info:" + productId;
        String priceKey = "product:prices:" + productId;

        // 1️⃣ [Redis] 상품 정보 조회
        ProductRedisDto infoDto = (ProductRedisDto) redisTemplate.opsForValue().get(infoKey);

        // 2️⃣ [Cache Miss] DB 조회 및 캐싱
        if (infoDto == null) {
            Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);

            infoDto = ProductRedisDto.from(product, options);
            redisTemplate.opsForValue().set(infoKey, infoDto, Duration.ofHours(24));
        }

        // 3️⃣ [Redis] 가격 조회 및 갱신
        Map<Object, Object> prices = redisTemplate.opsForHash().entries(priceKey);
        List<UUID> optionIds = infoDto.getOptions().stream()
                .map(ProductRedisDto.ProductOptionDto::getOptionId)
                .toList();
        fillMissingPrices(prices, optionIds, productId);

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
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return productMapper.toProductDetailResponseDto(infoDto, lowestPrice);
    }

    // 🔴 [TEST Method] 캐시 없이 DB와 Trade 서비스를 직접 찌르는 로직
    private ProductDetailResponseDto getProductDetailNoCache(UUID productId) {
        // 1. DB 조회
        Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductOption> options = productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId);

        // 2. DTO 변환 (기존 로직 재활용을 위해 ProductRedisDto 사용)
        ProductRedisDto infoDto = ProductRedisDto.from(product, options);

        // 3. Trade Service 직접 호출 (캐싱 X)
        List<UUID> optionIds = options.stream().map(ProductOption::getId).toList();

        BigDecimal lowestPrice = BigDecimal.ZERO;

        try {
            List<LowestPriceResponseDto> fetchedPrices = tradeClient.getLowestPrices(optionIds);

            // 최저가 계산
            lowestPrice = fetchedPrices.stream()
                    .map(dto -> dto.getLowestPrice() != null ? dto.getLowestPrice() : BigDecimal.ZERO)
                    .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

        } catch (Exception e) {
            log.warn("Trade service call failed in No-Cache mode for product {}", productId);
        }

        return productMapper.toProductDetailResponseDto(infoDto, lowestPrice);
    }

    // ✅ 상품 옵션 조회 (옵션별 최저가 포함) - Batch Optimization Applied
    @Override
    public List<ProductOptionListResponseDto> getProductOptions(UUID productId) {
        String infoKey = "product:info:" + productId;
        String priceKey = "product:prices:" + productId;

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
        if (!productRepository.existsByIdAndDeletedAtIsNull(productId)) {
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
            java.util.Set<String> returnedIds = new java.util.HashSet<>();

            for (LowestPriceResponseDto dto : fetched) {
                BigDecimal price = dto.getLowestPrice() != null ? dto.getLowestPrice() : BigDecimal.ZERO;
                String val = price.toString();
                String optionIdStr = dto.getProductOptionId().toString();

                prices.put(optionIdStr, val);
                newPrices.put(optionIdStr, val);
                returnedIds.add(optionIdStr);
            }

            // Trade 서비스에서 응답이 오지 않은(즉, 입찰 내역이 아예 없는) 옵션들도 0원으로 캐싱
            for (UUID id : missingIds) {
                String key = id.toString();
                if (!returnedIds.contains(key)) {
                    prices.put(key, BigDecimal.ZERO.toString());
                    newPrices.put(key, BigDecimal.ZERO.toString());
                }
            }

            // Redis Multi-set (Cache-aside)
            if (!newPrices.isEmpty()) {
                String priceKey = "product:prices:" + productId;
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
            log.warn("Invalid price in Redis. optionId={}, value={}", optionId, val, e);
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

