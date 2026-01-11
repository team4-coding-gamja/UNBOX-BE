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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    // 테스트용 더미 데이터 생성 헬퍼 메서드
    private Product createMockProduct() {
        Brand brand = Brand.createBrand("Test Brand", "https://logo.com");
        ReflectionTestUtils.setField(brand, "id", UUID.randomUUID());
        Product product = Product.createProduct("Test Product", "M-123", Category.SHOES, "https://image.com", brand);
        // ID 주입 (Reflection 사용)
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        return product;
    }

    @Nested
    @DisplayName("리뷰 관련 기능 테스트")
    class ReviewFeatureTest {

        @Test
        @DisplayName("리뷰 데이터 추가 성공 - 리뷰 수와 총점이 증가해야 한다")
        void addReviewData_Success() {
            // given
            UUID productId = UUID.randomUUID();
            int score = 5;
            Product product = createMockProduct(); // 초기: count 0, total 0

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            productService.addReviewData(productId, score);

            // then
            assertThat(product.getReviewCount()).isEqualTo(1);
            assertThat(product.getTotalScore()).isEqualTo(5);
            // findById가 호출되었는지 검증
            verify(productRepository).findById(productId);
        }

        @Test
        @DisplayName("리뷰 데이터 추가 실패 - 상품이 없으면 예외 발생")
        void addReviewData_Fail_ProductNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            given(productRepository.findById(productId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.addReviewData(productId, 5))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("리뷰 데이터 삭제 성공 - 리뷰 수와 총점이 감소해야 한다")
        void deleteReviewData_Success() {
            // given
            UUID productId = UUID.randomUUID();
            int score = 4;
            Product product = createMockProduct();

            // 미리 리뷰 1개를 추가해둠 (count: 1, total: 4)
            product.addReviewData(score);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            productService.deleteReviewData(productId, score);

            // then
            assertThat(product.getReviewCount()).isEqualTo(0);
            assertThat(product.getTotalScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("리뷰 데이터 수정 성공 - 기존 점수는 빠지고 새 점수가 더해져야 한다")
        void updateReviewData_Success() {
            // given
            UUID productId = UUID.randomUUID();
            int oldScore = 3;
            int newScore = 5;
            Product product = createMockProduct();

            // 초기 상태 설정 (count: 1, total: 3)
            product.addReviewData(oldScore);

            given(productRepository.findById(productId)).willReturn(Optional.of(product));

            // when
            productService.updateReviewData(productId, oldScore, newScore);

            // then
            assertThat(product.getReviewCount()).isEqualTo(1); // 개수는 그대로
            assertThat(product.getTotalScore()).isEqualTo(5); // 3 -> 5로 변경됨
        }

        @Test
        @DisplayName("리뷰 데이터 수정 최적화 - 점수가 같으면 DB 조회를 하지 않는다")
        void updateReviewData_Optimization_SameScore() {
            // given
            UUID productId = UUID.randomUUID();
            int oldScore = 5;
            int newScore = 5;

            // when
            productService.updateReviewData(productId, oldScore, newScore);

            // then
            // findById가 아예 호출되지 않았음을 검증
            verify(productRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("상품 조회 기능 테스트")
    class ProductReadTest {

        @Test
        @DisplayName("상품 목록 조회 - 필터링 및 최저가 매핑 확인")
        void getProducts_Success() {
            // given
            Product product = createMockProduct();
            UUID productId = product.getId();
            Pageable pageable = PageRequest.of(0, 10);
            List<Product> productList = Collections.singletonList(product);
            Page<Product> productPage = new PageImpl<>(productList);

            // Mock 1: 리포지토리 검색 결과
            given(productRepository.findByFiltersAndDeletedAtIsNull(any(), any(), any(), any()))
                    .willReturn(productPage);

            // Mock 2: 최저가 조회 결과 (Object 배열 리스트로 반환된다고 가정)
            // [productId, lowestPrice]
            List<Object[]> priceData = new ArrayList<>();
            priceData.add(new Object[]{productId, 150000});
            given(sellingBidRepository.findLowestPricesByProductIds(anyList()))
                    .willReturn(priceData);

            // Mock 3: 매퍼 동작
            ProductListResponseDto responseDto = ProductListResponseDto.builder()
                    .id(productId)
                    .name("Test Product")
                    .lowestPrice(150000)
                    .build();
            given(productMapper.toProductListResponseDto(any(Product.class), eq(150000)))
                    .willReturn(responseDto);

            // when
            Page<ProductListResponseDto> result = productService.getProducts(null, null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getLowestPrice()).isEqualTo(150000);
            verify(productRepository).findByFiltersAndDeletedAtIsNull(any(), any(), any(), any());
            verify(sellingBidRepository).findLowestPricesByProductIds(anyList());
        }

        @Test
        @DisplayName("필터링 테스트 - 조건에 맞는 상품이 없을 경우 빈 페이지 반환")
        void getProducts_Filter_EmptyResult() {
            // given
            String category = "ELECTRONICS"; // 가상의 카테고리
            String keyword = "없는상품";
            Pageable pageable = PageRequest.of(0, 10);

            // 검색 결과가 0건인 Page 객체 리턴
            given(productRepository.findByFiltersAndDeletedAtIsNull(any(), any(), eq(keyword), any()))
                    .willReturn(Page.empty());

            // when
            Page<ProductListResponseDto> result = productService.getProducts(null, category, keyword, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);

            // 최저가 조회 로직이 실행되지 않아야 함 (빈 리스트이므로 stream이 안 돔)
            verify(sellingBidRepository).findLowestPricesByProductIds(anyList());
        }

        @Test
        @DisplayName("상품 상세 조회 성공")
        void getProductDetail_Success() {
            // given
            Product product = createMockProduct();
            UUID productId = product.getId();
            ProductDetailResponseDto responseDto = ProductDetailResponseDto.builder()
                    .id(productId)
                    .name("Test Product")
                    .build();

            given(productRepository.findByIdAndDeletedAtIsNullWithBrand(productId))
                    .willReturn(Optional.of(product));
            given(productMapper.toProductDetailDto(product, 0))
                    .willReturn(responseDto);

            // when
            ProductDetailResponseDto result = productService.getProductDetail(productId);

            // then
            assertThat(result.getId()).isEqualTo(productId);
            assertThat(result.getName()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품")
        void getProductDetail_Fail() {
            // given
            UUID productId = UUID.randomUUID();
            given(productRepository.findByIdAndDeletedAtIsNullWithBrand(productId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.getProductDetail(productId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }
        @Test
        @DisplayName("상품 상세 조회 - 최저가 입찰이 없으면 0을 반환하는 브랜치를 탄다")
        void getProductDetail_NoPrice_Branch() {
            // given
            Product product = createMockProduct();
            given(productRepository.findByIdAndDeletedAtIsNullWithBrand(any())).willReturn(Optional.of(product));

            // 🚩 lowestPrice != null ? ... : 0 에서 null 브랜치 강제 발생
            given(sellingBidRepository.findLowestPriceByProductId(any(), any())).willReturn(null);

            // when
            productService.getProductDetail(product.getId());

            // then
            verify(productMapper).toProductDetailDto(product, 0);
        }
        @Test
        @DisplayName("상품 목록 조회 - 입찰 없는 상품이 섞여있어도 브랜치를 통과해야 한다")
        void getProducts_Success_WithMixedPrices() {
            // given
            Product product1 = createMockProduct();
            Product product2 = createMockProduct();
            Page<Product> productPage = new PageImpl<>(List.of(product1, product2));

            given(productRepository.findByFiltersAndDeletedAtIsNull(any(), any(), any(), any()))
                    .willReturn(productPage);

            List<Object[]> priceData = new ArrayList<>();
            priceData.add(new Object[]{product1.getId(), 150000}); // 정상가
            priceData.add(new Object[]{product2.getId(), null});   // 🚩 row[1] == null 브랜치 통과!

            given(sellingBidRepository.findLowestPricesByProductIds(anyList())).willReturn(priceData);

            // when
            productService.getProducts(null, null, null, PageRequest.of(0, 10));

            // then
            verify(productMapper).toProductListResponseDto(eq(product1), eq(150000));
            verify(productMapper).toProductListResponseDto(eq(product2), eq(0)); // getOrDefault(..., 0) 브랜치 통과
        }
    }

    @Nested
    @DisplayName("브랜드 조회 기능 테스트")
    class BrandReadTest {

        @Test
        @DisplayName("브랜드 전체 조회 성공")
        void getAllBrands_Success() {
            // given
            Brand brand1 = Brand.createBrand("Nike", "https://logo1.png");
            ReflectionTestUtils.setField(brand1, "id", UUID.randomUUID());
            Brand brand2 = Brand.createBrand("Adidas", "https://logo2.png");
            ReflectionTestUtils.setField(brand2, "id", UUID.randomUUID());
            List<Brand> brands = List.of(brand1, brand2);

            BrandListResponseDto dto1 = BrandListResponseDto.builder()
                    .id(brand1.getId())
                    .name("Nike")
                    .logoUrl("https://logo1.png")
                    .build();
            BrandListResponseDto dto2 = BrandListResponseDto.builder()
                    .id(brand2.getId())
                    .name("Adidas")
                    .logoUrl("https://logo2.png")
                    .build();

            given(brandRepository.findAll()).willReturn(brands);
            given(brandMapper.toBrandListDto(brand1)).willReturn(dto1);
            given(brandMapper.toBrandListDto(brand2)).willReturn(dto2);

            // when
            List<BrandListResponseDto> result = productService.getAllBrands();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Nike");
            verify(brandRepository).findAll();
        }

        @Test
        @DisplayName("브랜드 전체 조회 실패 - DB 에러")
        void getAllBrands_Fail_DBError() {
            // given
            given(brandRepository.findAll()).willThrow(new RuntimeException("DB Error"));

            // when & then
            assertThatThrownBy(() -> productService.getAllBrands())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB Error");
        }
    }

    @Nested
    @DisplayName("옵션별 최저가 조회 테스트")
    class ProductOptionPriceTest {

        @Test
        @DisplayName("옵션 목록 및 최저가 매핑 성공")
        void getProductOptions_Success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId1 = UUID.randomUUID();
            UUID optionId2 = UUID.randomUUID();

            // 1. 상품 존재 확인 Mock
            given(productRepository.existsById(productId)).willReturn(true);

            // 2. 옵션 목록 Mock (옵션 2개 가정)
            Product product = createMockProduct();
            ProductOption option1 = ProductOption.createProductOption(product, "260");
            ReflectionTestUtils.setField(option1, "id", optionId1);
            ProductOption option2 = ProductOption.createProductOption(product, "270");
            ReflectionTestUtils.setField(option2, "id", optionId2);

            given(productOptionRepository.findAllByProductIdAndDeletedAtIsNull(productId))
                    .willReturn(List.of(option1, option2));

            // 3. 최저가 데이터 Mock (optionId1은 20만원, optionId2는 입찰 없음)
            List<Object[]> priceData = new ArrayList<>();
            priceData.add(new Object[]{optionId1, 200000});
            // option2는 데이터가 없음

            given(sellingBidRepository.findLowestPriceByOptionIds(anyList()))
                    .willReturn(priceData);

            // 4. Mapper Mock
            ProductOptionListResponseDto dto1 = ProductOptionListResponseDto.builder()
                    .id(optionId1).lowestPrice(200000).build();
            ProductOptionListResponseDto dto2 = ProductOptionListResponseDto.builder()
                    .id(optionId2).lowestPrice(0).build();

            given(productMapper.toProductOptionListDto(eq(option1), eq(200000))).willReturn(dto1);
            // 서비스 코드에서 getOrDefault(..., 0)을 사용하므로 0을 전달해야 함
            given(productMapper.toProductOptionListDto(eq(option2), eq(0))).willReturn(dto2);

            // when
            List<ProductOptionListResponseDto> result = productService.getProductOptions(productId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLowestPrice()).isEqualTo(200000);
            assertThat(result.get(1).getLowestPrice()).isEqualTo(0);

            verify(productRepository).existsById(productId);
            verify(sellingBidRepository).findLowestPriceByOptionIds(anyList());
        }

        @Test
        @DisplayName("옵션 조회 실패 - 상품이 존재하지 않음")
        void getProductOptions_Fail_ProductNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            given(productRepository.existsById(productId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> productService.getProductOptions(productId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

            // 검증: 상품이 없으면 이후 로직은 실행되면 안됨
            verify(productOptionRepository, never()).findAllByProductIdAndDeletedAtIsNull(any());
        }
    }
    @Test
    @DisplayName("단건 상품에 대해 최저가 입찰이 없는 경우 0원을 반환해야 함")
    void getProductDetail_Success_NoLowestPrice() {
        // given
        Product product = createMockProduct();
        UUID productId = product.getId();

        given(productRepository.findByIdAndDeletedAtIsNullWithBrand(productId))
                .willReturn(Optional.of(product));
        // 🚩 최저가 조회 결과가 null인 상황 시뮬레이션
        given(sellingBidRepository.findLowestPriceByProductId(eq(productId), any()))
                .willReturn(null);

        // then: Mapper에 0이 전달되는지 확인
        productService.getProductDetail(productId);
        verify(productMapper).toProductDetailDto(product, 0);
    }






}