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
import com.example.unbox_be.domain.trade.service.TradeService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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
public class ProductServiceImpl implements  ProductService {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final BrandMapper brandMapper;
    private final SellingBidRepository sellingBidRepository;
    private final TradeService tradeService;

    // ✅ 상품 목록 조회 (검색 + 페이징)
    public Page<ProductListResponseDto> getProducts(UUID brandId, String category, String keyword, Pageable pageable) {
        Category parsedCategory = Category.fromNullable(category);

        Page<Product> page = productRepository.findByFilters(brandId, parsedCategory, keyword, pageable);

        // ✅ 최저가(lowestPrice) 계산은 Trade 모듈이 있어야 정확히 가능.
        // 지금은 “정상 동작”을 위해 null(또는 0)로 내려주고,
        // 추후 TradeService 붙이면 여기서 매핑 시 lowestPrice를 채우면 됨.
        return page.map(p -> productMapper.toProductListDto(p, null));
    }

    // ✅ 상품 상세 조회
    public ProductDetailResponseDto getProductDetail(UUID productId) {
        Product product = productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toProductDetailDto(product, null);
    }

    // ✅ 상품 옵션별 최저가 조회
    @Override
    public List<ProductOptionListResponseDto> getProductOptions(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductOption> options = productOptionRepository.findAllByProductId(productId);
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
                                lowestPriceMap.get(option.getId())
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
}
