package com.example.unbox_product.product.application.service;

// import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDtoV2;
import com.example.unbox_product.product.presentation.dto.response.SliceResponse;
import com.example.unbox_product.product.presentation.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestProductServiceImpl implements TestProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String POPULAR_ZSET_KEY = "products:popular:all";

    @Override
    public SliceResponse<ProductListResponseDtoV2> getProductsV2(Long lastScore, UUID lastId, UUID brandId, int size) {
        // 1. Redis ZSET에서 ID 리스트 조회 (Score 기반 No-Offset)
        // brandId가 있는 경우 별도의 ZSET 키를 사용하거나 DB로 바로 보낼 수 있습니다.
        String zsetKey = (brandId == null) ? POPULAR_ZSET_KEY : "products:popular:brand:" + brandId;

        double maxScore = (lastScore == null) ? Double.MAX_VALUE : lastScore.doubleValue();

        // size + 1개를 가져와서 다음 페이지 여부 확인
        Set<Object> productIds = redisTemplate.opsForZSet().reverseRangeByScore(zsetKey, 0, maxScore, 0, size + 1);

        if (productIds != null && !productIds.isEmpty()) {
            List<String> detailKeys = productIds.stream()
                    .map(id -> PRODUCT_DETAIL_KEY_PREFIX + id.toString())
                    .collect(Collectors.toList());

            // 2. Multi-Get으로 상세 정보 캐시 조회
            List<Object> cachedData = redisTemplate.opsForValue().multiGet(detailKeys);

            // 모든 데이터가 캐시에 존재하는지 확인 (null이 없어야 함)
            if (cachedData != null && !cachedData.contains(null)) {
                List<ProductListResponseDtoV2> dtoList = cachedData.stream()
                        .map(obj -> (ProductListResponseDtoV2) obj)
                        .limit(size)
                        .collect(Collectors.toList());

                boolean hasNext = cachedData.size() > size;
                return new SliceResponse<>(dtoList, hasNext);
            }
        }

        // 3. 캐시 미스 시 DB 조회
        log.info("Cache Miss - Fetching from DB: lastScore={}, brandId={}", lastScore, brandId);
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Product> productSlice = productRepository.findByPopularityNoOffset(lastScore, lastId, brandId,
                pageRequest);

        List<ProductListResponseDtoV2> dtoList = productSlice.getContent().stream()
                .map(productMapper::toProductListResponseDtoV2)
                .collect(Collectors.toList());

        // 4. 조회된 데이터를 Redis에 다시 채우기 (Async 권장이나 여기선 동기 처리)
        cacheProducts(dtoList, zsetKey);

        return new SliceResponse<>(dtoList, productSlice.hasNext());
    }

    private void cacheProducts(List<ProductListResponseDtoV2> dtoList, String zsetKey) {
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            for (ProductListResponseDtoV2 dto : dtoList) {
                String detailKey = PRODUCT_DETAIL_KEY_PREFIX + dto.getProductId();
                // 개별 상세 정보 캐싱 (TTL 1시간)
                redisTemplate.opsForValue().set(detailKey, dto, Duration.ofHours(1));
                // ZSET 순위 정보 갱신
                redisTemplate.opsForZSet().add(zsetKey, dto.getProductId().toString(), dto.getPopularityScore());
            }
            return null;
        });
    }
}