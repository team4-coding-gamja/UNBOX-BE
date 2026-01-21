package com.example.unbox_product.product.application.service;

import com.example.unbox_product.product.presentation.dto.redis.ProductRedisDto;
import com.example.unbox_product.product.presentation.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductMapper productMapper;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Test
    void getProductDetail_shouldCalculateLowestPrice() {
        // Given
        UUID productId = UUID.randomUUID();
        ProductRedisDto infoDto = new ProductRedisDto(
                productId, "Name", "Model", "Image",
                UUID.randomUUID(), "Brand", null, 0, 0.0,
                Collections.emptyList());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        when(valueOperations.get("prod:info:" + productId)).thenReturn(infoDto);

        Map<Object, Object> prices = new HashMap<>();
        prices.put("option1", "15000");
        prices.put("option2", "10000");
        when(hashOperations.entries("prod:prices:" + productId)).thenReturn(prices);

        // When
        productService.getProductDetail(productId);

        // Then
        // Expect lowest price 10000
        verify(productMapper).toProductDetailResponseDto(any(), eq(new BigDecimal("10000")));
    }

    @Test
    void getProductOptions_shouldMapPricesCorrectly_CacheHit() {
        // Given
        UUID productId = UUID.randomUUID();
        UUID optionId1 = UUID.randomUUID();

        ProductRedisDto.ProductOptionDto optionDto1 = new ProductRedisDto.ProductOptionDto(optionId1, "Option 1");
        ProductRedisDto infoDto = new ProductRedisDto(
                productId, "Name", "Model", "Image",
                UUID.randomUUID(), "Brand", null, 0, 0.0,
                List.of(optionDto1));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        when(valueOperations.get("prod:info:" + productId)).thenReturn(infoDto);

        Map<Object, Object> prices = new HashMap<>();
        prices.put(optionId1.toString(), "20000");
        when(hashOperations.entries("prod:prices:" + productId)).thenReturn(prices);

        // When
        productService.getProductOptions(productId);

        // Then
        verify(productMapper).toProductOptionListDtoFromRedis(eq(optionDto1), eq(new BigDecimal("20000")));
    }
}
