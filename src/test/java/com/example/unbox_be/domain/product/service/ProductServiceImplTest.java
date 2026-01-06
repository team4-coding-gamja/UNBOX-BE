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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private SellingBidRepository sellingBidRepository;

    @Test
    @DisplayName("상품 목록 조회 - 성공")
    void getProducts_success() {
        // given
        UUID brandId = UUID.randomUUID();
        String category = "SHOES";
        String keyword = "Nike";
        Pageable pageable = PageRequest.of(0, 10);

        Product product = mock(Product.class);
        Page<Product> productPage = new PageImpl<>(List.of(product));

        given(productRepository.findByFilters(eq(brandId), eq(Category.SHOES), eq(keyword), eq(pageable)))
                .willReturn(productPage);

        ProductListResponseDto responseDto = mock(ProductListResponseDto.class);
        given(productMapper.toProductListDto(eq(product), any())).willReturn(responseDto);

        // when
        Page<ProductListResponseDto> result = productService.getProducts(brandId, category, keyword, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(responseDto);
    }

//    @Test
//    @DisplayName("상품 상세 조회 - 성공")
//    void getProductDetail_success() {
//        // given
//        UUID productId = UUID.randomUUID();
//        Product product = mock(Product.class);
//
//        given(productRepository.findByIdWithBrand(productId)).willReturn(Optional.of(product));
//
//        ProductDetailResponseDto responseDto = mock(ProductDetailResponseDto.class);
//        given(productMapper.toProductDetailDto(eq(product), any())).willReturn(responseDto);
//
//        // when
//        ProductDetailResponseDto result = productService.getProductDetail(productId);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result).isEqualTo(responseDto);
//    }

//    @Test
//    @DisplayName("상품 상세 조회 - 실패 (상품 없음)")
//    void getProductDetail_notFound() {
//        // given
//        UUID productId = UUID.randomUUID();
//        given(productRepository.findByIdWithBrand(productId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> productService.getProductDetail(productId))
//                .isInstanceOf(CustomException.class)
//                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
//    }
//
//    @Test
//    @DisplayName("상품 옵션별 최저가 조회 - 성공")
//    void getProductOptions_success() {
//        // given
//        UUID productId = UUID.randomUUID();
//        UUID optionId = UUID.randomUUID();
//
//        given(productRepository.existsById(productId)).willReturn(true);
//
//        ProductOption option = mock(ProductOption.class);
//        given(option.getId()).willReturn(optionId);
//        given(productOptionRepository.findAllByProductId(productId)).willReturn(List.of(option));
//
//        // Mocking SellingBidRepository result
//        List<Object[]> lowestPrices = new ArrayList<>();
//        lowestPrices.add(new Object[]{optionId, 10000});
//        given(sellingBidRepository.findLowestPriceByOptionIds(any())).willReturn(lowestPrices);
//
//        ProductOptionListResponseDto responseDto = mock(ProductOptionListResponseDto.class);
//        given(productMapper.toProductOptionListDto(eq(option), eq(10000))).willReturn(responseDto);
//
//        // when
//        List<ProductOptionListResponseDto> result = productService.getProductOptions(productId);
//
//        // then
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0)).isEqualTo(responseDto);
//    }

    @Test
    @DisplayName("상품 옵션별 최저가 조회 - 실패 (상품 없음)")
    void getProductOptions_productNotFound() {
        // given
        UUID productId = UUID.randomUUID();
        given(productRepository.existsById(productId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> productService.getProductOptions(productId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("브랜드 전체 조회 - 성공")
    void getAllBrands_success() {
        // given
        Brand brand = mock(Brand.class);
        given(brandRepository.findAll()).willReturn(List.of(brand));

        BrandListResponseDto responseDto = mock(BrandListResponseDto.class);
        given(brandMapper.toBrandListDto(brand)).willReturn(responseDto);

        // when
        List<BrandListResponseDto> result = productService.getAllBrands();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(responseDto);
    }
}
