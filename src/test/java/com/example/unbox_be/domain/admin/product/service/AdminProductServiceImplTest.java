package com.example.unbox_be.domain.admin.product.service;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.ProductSearchCondition;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.admin.product.mapper.AdminProductMapper;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Category;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AdminProductServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock BrandRepository brandRepository;
    @Mock AdminProductMapper adminProductMapper;

    @InjectMocks AdminProductServiceImpl adminProductService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 상품목록조회_조건없이요청하면_findByFiltersAndDeletedAtIsNull을호출하고_매핑된페이지를반환한다() {
        // given
        ProductSearchCondition condition = ProductSearchCondition.builder()
                .brandId(null)
                .category(null)
                .keyword(null)
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Product p1 = mock(Product.class);
        Product p2 = mock(Product.class);

        Page<Product> productPage = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(productRepository.findByFiltersAndDeletedAtIsNull(
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(productPage);

        AdminProductListResponseDto dto1 = mock(AdminProductListResponseDto.class);
        AdminProductListResponseDto dto2 = mock(AdminProductListResponseDto.class);

        when(adminProductMapper.toAdminProductListResponseDto(p1)).thenReturn(dto1);
        when(adminProductMapper.toAdminProductListResponseDto(p2)).thenReturn(dto2);

        // when
        Page<AdminProductListResponseDto> result = adminProductService.getProducts(condition, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(dto1, dto2);

        verify(productRepository).findByFiltersAndDeletedAtIsNull(isNull(), isNull(), isNull(), eq(pageable));
        verify(adminProductMapper).toAdminProductListResponseDto(p1);
        verify(adminProductMapper).toAdminProductListResponseDto(p2);
    }


    @Test
    void 상품등록_브랜드가없으면_BRAND_NOT_FOUND_예외발생() {
        // given
        UUID brandId = UUID.randomUUID();

        AdminProductCreateRequestDto dto = AdminProductCreateRequestDto.builder()
                .brandId(brandId)
                .name("상품명")
                .modelNumber("MODEL-001")
                .category(Category.SHOES)
                .imageUrl("http://img")
                .build();

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.createProduct(dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BRAND_NOT_FOUND);
                });

        verify(productRepository, never()).save(any());
        verify(adminProductMapper, never()).toAdminProductCreateResponseDto(any());
    }

    @Test
    void 상품등록_정상요청이면_저장성공하고_응답을반환한다() {
        // given
        UUID brandId = UUID.randomUUID();
        Brand brand = mock(Brand.class);

        AdminProductCreateRequestDto dto = AdminProductCreateRequestDto.builder()
                .brandId(brandId)
                .name("상품명")
                .modelNumber("MODEL-001")
                .category(Category.SHOES)
                .imageUrl("http://img")
                .build();

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.of(brand));

        Product savedProduct = mock(Product.class);
        when(productRepository.save(any(Product.class)))
                .thenReturn(savedProduct);

        AdminProductCreateResponseDto response = mock(AdminProductCreateResponseDto.class);
        when(adminProductMapper.toAdminProductCreateResponseDto(savedProduct))
                .thenReturn(response);

        // when
        AdminProductCreateResponseDto result = adminProductService.createProduct(dto);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isSameAs(response);

        verify(brandRepository).findByIdAndDeletedAtIsNull(brandId);
        verify(productRepository).save(any(Product.class));
        verify(adminProductMapper).toAdminProductCreateResponseDto(savedProduct);
    }

    @Test
    void 상품수정_상품이없으면_PRODUCT_NOT_FOUND_예외발생() {
        // given
        UUID productId = UUID.randomUUID();
        AdminProductUpdateRequestDto dto = mock(AdminProductUpdateRequestDto.class);

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });

        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verifyNoMoreInteractions(productRepository);
        verifyNoInteractions(adminProductMapper);
    }

    @Test
    void 상품수정_모델넘버가중복이면_PRODUCT_MODEL_NUMBER_ALREADY_EXISTS_예외발생() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = mock(Product.class);

        AdminProductUpdateRequestDto dto = mock(AdminProductUpdateRequestDto.class);
        when(dto.getModelNumber()).thenReturn("MDL-123");

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.of(product));

        when(productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull("MDL-123", productId))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminProductService.updateProduct(productId, dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_MODEL_NUMBER_ALREADY_EXISTS);
                });

        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verify(productRepository).existsByModelNumberAndIdNotAndDeletedAtIsNull("MDL-123", productId);
        verifyNoInteractions(adminProductMapper);
        verify(product, never()).update(any(), any(), any(), any());
    }

    @Test
    void 상품수정_정상요청이면_update가호출되고_응답dto를반환한다() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = mock(Product.class);

        AdminProductUpdateRequestDto dto = mock(AdminProductUpdateRequestDto.class);
        when(dto.getName()).thenReturn("새상품명");
        when(dto.getModelNumber()).thenReturn("MDL-123");
        when(dto.getCategory()).thenReturn(Category.SHOES); // ✅ 네 프로젝트에서 category 타입이 String이면 그대로, Enum이면 Enum으로 맞춰줘
        when(dto.getImageUrl()).thenReturn("http://img.com/1.png");

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.of(product));

        // 모델넘버 중복 체크 false
        when(productRepository.existsByModelNumberAndIdNotAndDeletedAtIsNull("MDL-123", productId))
                .thenReturn(false);

        AdminProductUpdateResponseDto response = mock(AdminProductUpdateResponseDto.class);
        when(adminProductMapper.toAdminProductUpdateResponseDto(product))
                .thenReturn(response);

        // when
        AdminProductUpdateResponseDto result = adminProductService.updateProduct(productId, dto);

        // then
        assertThat(result).isSameAs(response);

        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verify(productRepository).existsByModelNumberAndIdNotAndDeletedAtIsNull("MDL-123", productId);

        verify(product).update("새상품명", "MDL-123", Category.SHOES, "http://img.com/1.png");
        verify(adminProductMapper).toAdminProductUpdateResponseDto(product);
    }

    @Test
    void 상품수정_모델넘버가null이면_중복체크없이_update가호출된다() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = mock(Product.class);

        AdminProductUpdateRequestDto dto = mock(AdminProductUpdateRequestDto.class);
        when(dto.getName()).thenReturn("새상품명");
        when(dto.getModelNumber()).thenReturn(null); // ✅ 핵심
        when(dto.getCategory()).thenReturn(Category.SHOES);
        when(dto.getImageUrl()).thenReturn("http://img.com/1.png");

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.of(product));

        AdminProductUpdateResponseDto response = mock(AdminProductUpdateResponseDto.class);
        when(adminProductMapper.toAdminProductUpdateResponseDto(product))
                .thenReturn(response);

        // when
        AdminProductUpdateResponseDto result = adminProductService.updateProduct(productId, dto);

        // then
        assertThat(result).isSameAs(response);

        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verify(productRepository, never())
                .existsByModelNumberAndIdNotAndDeletedAtIsNull(anyString(), any());

        verify(product).update("새상품명", null, Category.SHOES, "http://img.com/1.png");
        verify(adminProductMapper).toAdminProductUpdateResponseDto(product);
    }

    @Test
    void 상품삭제_상품이있으면_deletedBy로_softDelete가호출된다() {
        // given
        UUID productId = UUID.randomUUID();
        String deletedBy = "master@test.com";

        Product product = mock(Product.class);

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.of(product));

        // when
        adminProductService.deleteProduct(productId, deletedBy);

        // then
        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verify(product).softDelete(deletedBy);
    }

    @Test
    void 상품삭제_상품이없으면_PRODUCT_NOT_FOUND_예외발생() {
        // given
        UUID productId = UUID.randomUUID();
        String deletedBy = "master@test.com";

        when(productRepository.findByIdAndDeletedAtIsNull(productId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminProductService.deleteProduct(productId, deletedBy))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
                });

        verify(productRepository).findByIdAndDeletedAtIsNull(productId);
        verifyNoMoreInteractions(productRepository);
    }
}
