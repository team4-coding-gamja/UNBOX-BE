package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.domain.entity.Category;
import com.example.unbox_product.product.domain.entity.Product;
import com.example.unbox_product.product.domain.repository.ProductRepository;
import com.example.unbox_product.product.presentation.dto.response.ProductListResponseDto;
import com.example.unbox_product.product.presentation.dto.response.SliceResponse;
import com.example.unbox_product.product.presentation.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestProductServiceImpl implements TestProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public SliceResponse<ProductListResponseDto> getProductsV2(UUID lastProductId, UUID brandId, String category, String keyword, int size) {
        String cacheKey = "products:v2:" + Objects.toString(brandId, "all") + ":" + Objects.toString(category, "all") + ":" + keyword;

        // 1. Redis에서 SliceResponse로 꺼내기
        if (lastProductId == null) {
            SliceResponse<ProductListResponseDto> cached = (SliceResponse<ProductListResponseDto>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return cached;
        }

        // 2. DB 조회
        Category categoryEnum = Category.fromNullable(category);
        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Product> productSlice = productRepository.findByNoOffset(lastProductId, brandId, categoryEnum, keyword, pageRequest);

        // 3. DTO 변환 후 SliceResponse로 감싸기
        SliceResponse<ProductListResponseDto> result = SliceResponse.from(
                productSlice.map(productMapper::toProductListResponseDto)
        );

        // 4. 캐싱 및 반환
        if (lastProductId == null) {
            redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));
        }
        return result;
    }
}