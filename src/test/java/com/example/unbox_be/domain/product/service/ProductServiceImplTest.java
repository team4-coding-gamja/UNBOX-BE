//package com.example.unbox_be.domain.product.service;
//
//import com.example.unbox_be.domain.product.dto.response.ProductListResponseDto;
//import com.example.unbox_be.domain.product.dto.ProductSearchCondition;
//import com.example.unbox_be.domain.product.entity.Brand;
//import com.example.unbox_be.domain.product.entity.Category;
//import com.example.unbox_be.domain.product.entity.Product;
//import com.example.unbox_be.domain.product.entity.ProductOption;
//import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
//import com.example.unbox_be.domain.product.repository.ProductRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class ProductServiceImplTest {
//
//    @InjectMocks
//    private ProductServiceImpl productService;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    @Mock
//    private ProductOptionRepository productOptionRepository;
//
//    @Test
//    @DisplayName("상품 검색 조회 성공 - 옵션 매핑 확인")
//    void getProducts_success() {
//        // given
//        // 1. 가짜 데이터 생성 (Reflection이나 Mock을 써도 되지만, 이해를 위해 Spy나 Mock 이용)
//        Product product1 = mock(Product.class);
//        Brand brand1 = Brand.createBrand("Nike","https://nike.com");
//
//        given(product1.getId()).willReturn(UUID.randomUUID());
//        given(product1.getName()).willReturn("Air Force 1");
//        given(product1.getBrand()).willReturn(brand1);
//        given(product1.getCategory()).willReturn(Category.SHOES);
//
//        ProductOption option1 = new ProductOption(product1, "260");
//        ProductOption option2 = new ProductOption(product1, "270");
//
//        Pageable pageable = PageRequest.of(0, 10);
//        ProductSearchCondition condition = new ProductSearchCondition();
//
//        // 2. Mocking (리포지토리 동작 정의)
//        // 검색 결과로 product1을 담은 페이지 반환
//        given(productRepository.search(any(), any())).willReturn(new PageImpl<>(List.of(product1)));
//        // 옵션 조회 시 option1, option2 반환
//        given(productOptionRepository.findAllByProductIdInAndDeletedAtIsNullIn(anyList())).willReturn(List.of(option1, option2));
//
//        // when
//        Page<ProductListResponseDto> result = productService.getProducts(condition, pageable);
//
//        // then
//        assertThat(result.getContent()).hasSize(1);
//        assertThat(result.getContent().get(0).getName()).isEqualTo("Air Force 1");
//        assertThat(result.getContent().get(0).getBrand().getName()).isEqualTo("Nike");
//
//        // ** 핵심 검증: 옵션이 DTO에 잘 들어갔는지 **
//        assertThat(result.getContent().get(0).getOptions()).hasSize(2);
//        assertThat(result.getContent().get(0).getOptions()).contains("260", "270");
//
//        // 리포지토리 호출 횟수 검증
//        verify(productRepository, times(1)).search(any(), any());
//        verify(productOptionRepository, times(1)).findAllByProductIdInAndDeletedAtIsNullIn(anyList());
//    }
//
//    @Test
//    @DisplayName("상품 검색 결과가 없을 때 - 옵션 조회 쿼리가 실행되지 않아야 함")
//    void getProducts_empty() {
//        // given
//        Pageable pageable = PageRequest.of(0, 10);
//        ProductSearchCondition condition = new ProductSearchCondition();
//
//        // 빈 페이지 반환
//        given(productRepository.search(any(), any())).willReturn(Page.empty());
//
//        // when
//        Page<ProductListResponseDto> result = productService.getProducts(condition, pageable);
//
//        // then
//        assertThat(result).isEmpty();
//
//        // ** 핵심 검증: 방어 코드가 작동하여 옵션 레포지토리는 호출되지 않아야 함 **
//        verify(productOptionRepository, never()).findAllByProductIdInAndDeletedAtIsNullIn(anyList());
//    }
//
//    @Test
//    @DisplayName("상품 상세 조회 성공")
//    void getProductById_success() {
//        // given
//        UUID productId = UUID.randomUUID();
//        Product product = mock(Product.class);
//        Brand brand = Brand.createBrand("Adidas", "https://adidas.com");
//
//        given(product.getId()).willReturn(productId);
//        given(product.getName()).willReturn("Superstar");
//        given(product.getBrand()).willReturn(brand);
//
//        ProductOption option = new ProductOption(product, "250");
//
//        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));
//        given(productOptionRepository.findAllByProductIdInAndDeletedAtIsNull(productId)).willReturn(List.of(option));
//
//        // when
//        ProductListResponseDto result = productService.getProductById(productId);
//
//        // then
//        assertThat(result.getName()).isEqualTo("Superstar");
//        assertThat(result.getOptions()).contains("250");
//    }
//}
