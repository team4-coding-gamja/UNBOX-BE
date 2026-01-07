package com.example.unbox_be.domain.product.service;

import com.example.unbox_be.domain.product.dto.response.BrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.mapper.BrandMapper;
import com.example.unbox_be.domain.product.mapper.ProductMapper;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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

    // ✅ 상품 목록 조회 (검색 + 페이징)
    public Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable) {
        Category parsedCategory = Category.fromNullable(category);

        // 1) category 문자열 -> Category Enum 변환 (null/빈값이면 필터 미적용)
        Category categoryEnum = Category.fromNullable(category);

        // 2) Repository 검색 + 페이징
        Page<Product> products = productRepository.findByFiltersAndDeletedAtIsNull(brandId, categoryEnum, keyword, pageable);

        // 3) 최저가 조회 (N+1 문제 방지: 한 번의 쿼리로 조회)
        List<UUID> productIds = products.getContent().stream()
                .map(Product::getId)
                .toList();

        // productId별 최저가 Map 생성 (key: productId, value: lowestPrice)
        Map<UUID, Integer> lowestPriceMap =
                sellingBidRepository.findLowestPricesByProductIds(productIds).stream()
                        .collect(Collectors.toMap(
                                row -> (UUID) row[0],
                                row -> row[1] == null ? null : ((Number) row[1]).intValue()
                        ));

        return products.map(p ->
                productMapper.toProductListResponseDto(
                        p, lowestPriceMap.getOrDefault(p.getId(), 0)
                )
        );
    }

    // ✅ 상품 상세 조회
    public ProductDetailResponseDto getProductDetail(UUID productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNullWithBrand(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toProductDetailDto(product, null);
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
                        row -> ((Number) row[1]).intValue()
                ));

        return options.stream()
                .map(option ->
                        productMapper.toProductOptionListDto(
                                option,
                                lowestPriceMap.getOrDefault(option.getId(), 0)
                        )
                )
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
    public void addReviewData(UUID productId, int score){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.addReviewData(score);
    }

    @Transactional
    public void deleteReviewData(UUID productId, int score){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.deleteReviewData(score);
    }

    @Transactional
    public void updateReviewData(UUID productId, int oldScore, int newScore) {
        if (oldScore == newScore) return;

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        product.updateReviewData(oldScore, newScore);
    }
}
