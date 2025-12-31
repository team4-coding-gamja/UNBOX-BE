package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.ProductResponseDto;
import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    // 상품 전체 조회 (페이징 처리)
    public Page<ProductResponseDto> getProducts(ProductSearchCondition condition, Pageable pageable){
        // 1. 상품 조회 (쿼리 1번)
        Page<Product> productPage = productRepository.search(condition, pageable);
        List<Product> productList = productPage.getContent();
        // [방어 코드] 조회된 상품이 없으면 빈 페이지 반환 (옵션 조회 쿼리 방지)
        if (productList.isEmpty()) {
            return productPage.map(product -> ProductResponseDto.from(product, List.of()));
        }
        List<UUID> productIds = productList.stream()
                .map(Product::getId)
                .toList();
        // 2. 옵션 조회 (쿼리 1번) -> 메서드 명에 'In' 추가
        List<ProductOption> optionList = productOptionRepository.findAllByProductIdIn(productIds);
        // 3. Map 변환 (메모리 작업)
        Map<UUID, List<ProductOption>> optionMap = optionList.stream()
                .collect(Collectors.groupingBy(opt -> opt.getProduct().getId()));
        // 4. DTO 변환
        return productPage.map(product ->
                ProductResponseDto.from(product, optionMap.getOrDefault(product.getId(), List.of()))
        );
    }

    // 상품 상세 조회
    public ProductResponseDto getProductById(UUID id) {
        // 1. 상품 조회
        Product product = productRepository.findById(id).orElseThrow();
        // 2. 옵션 조회
        List<ProductOption> optionList = productOptionRepository.findAllByProductId(product.getId());
        // 3. DTO 변환
        return ProductResponseDto.from(product, optionList);
    }
}
