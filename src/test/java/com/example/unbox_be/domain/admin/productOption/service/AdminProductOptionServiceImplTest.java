package com.example.unbox_be.domain.admin.productOption.service;

import com.example.unbox_be.domain.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductOptionListResponseDto;
import com.example.unbox_be.domain.product.mapper.AdminProductOptionMapper;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.product.repository.ProductRepository;
import com.example.unbox_be.domain.product.service.AdminProductOptionServiceImpl;
import com.example.unbox_be.global.error.exception.CustomException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductOptionServiceImplTest {

    @InjectMocks
    private AdminProductOptionServiceImpl adminProductOptionService;

    @Mock private ProductRepository productRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private AdminProductOptionMapper adminProductMapper;

    @Captor
    ArgumentCaptor<ProductOption> productOptionCaptor;

    @Nested
    class 상품_옵션_목록_조회_테스트 {

        @Test
        void 상품_ID가_있으면_해당_상품의_옵션_목록을_조회하고_DTO로_변환한다() {
            // given
            UUID productId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            ProductOption option1 = mock(ProductOption.class);
            ProductOption option2 = mock(ProductOption.class);

            Page<ProductOption> repoPage = new PageImpl<>(List.of(option1, option2), pageable, 2);

            when(productOptionRepository.findByProductIdAndDeletedAtIsNull(eq(productId), eq(pageable)))
                    .thenReturn(repoPage);

            AdminProductOptionListResponseDto dto1 = mock(AdminProductOptionListResponseDto.class);
            AdminProductOptionListResponseDto dto2 = mock(AdminProductOptionListResponseDto.class);
            when(adminProductMapper.toAdminProductOptionResponseDto(option1)).thenReturn(dto1);
            when(adminProductMapper.toAdminProductOptionResponseDto(option2)).thenReturn(dto2);

            // when
            Page<AdminProductOptionListResponseDto> result =
                    adminProductOptionService.getProductOptions(productId, pageable);

            // then
            verify(productOptionRepository, times(1)).findByProductIdAndDeletedAtIsNull(productId, pageable);
            verify(productOptionRepository, never()).findAllByDeletedAtIsNull(any());

            verify(adminProductMapper, times(1)).toAdminProductOptionResponseDto(option1);
            verify(adminProductMapper, times(1)).toAdminProductOptionResponseDto(option2);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).containsExactly(dto1, dto2);
        }

        @Test
        void 상품_ID가_없으면_삭제되지_않은_전체_옵션_목록을_조회하고_DTO로_변환한다() {
            // given
            Pageable pageable = PageRequest.of(1, 30);

            ProductOption option1 = mock(ProductOption.class);
            Page<ProductOption> repoPage = new PageImpl<>(List.of(option1), pageable, 1);

            when(productOptionRepository.findAllByDeletedAtIsNull(eq(pageable)))
                    .thenReturn(repoPage);

            AdminProductOptionListResponseDto dto1 = mock(AdminProductOptionListResponseDto.class);
            when(adminProductMapper.toAdminProductOptionResponseDto(option1)).thenReturn(dto1);

            // when
            Page<AdminProductOptionListResponseDto> result =
                    adminProductOptionService.getProductOptions(null, pageable);

            // then
            verify(productOptionRepository, times(1)).findAllByDeletedAtIsNull(pageable);
            verify(productOptionRepository, never()).findByProductIdAndDeletedAtIsNull(any(), any());

            verify(adminProductMapper, times(1)).toAdminProductOptionResponseDto(option1);
            assertThat(result.getContent()).containsExactly(dto1);
        }

        @Test
        void 조회_결과가_비어있으면_매퍼를_호출하지_않고_빈_페이지를_반환한다() {
            // given
            UUID productId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Page<ProductOption> empty = Page.empty(pageable);
            when(productOptionRepository.findByProductIdAndDeletedAtIsNull(eq(productId), eq(pageable)))
                    .thenReturn(empty);

            // when
            Page<AdminProductOptionListResponseDto> result =
                    adminProductOptionService.getProductOptions(productId, pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
            verify(adminProductMapper, never()).toAdminProductOptionResponseDto(any());
        }
    }

    @Nested
    class 상품_옵션_등록_테스트 {

        @Test
        void 상품_존재_및_옵션_중복이_없으면_성공적으로_저장하고_응답_DTO를_반환한다() {
            // given
            UUID productId = UUID.randomUUID();

            Product product = mock(Product.class);
            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));

            AdminProductOptionCreateRequestDto requestDto = mock(AdminProductOptionCreateRequestDto.class);
            when(requestDto.getOption()).thenReturn("270");

            when(productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "270"))
                    .thenReturn(false);

            ProductOption saved = mock(ProductOption.class);
            when(productOptionRepository.save(any(ProductOption.class))).thenReturn(saved);

            AdminProductOptionCreateResponseDto responseDto = mock(AdminProductOptionCreateResponseDto.class);
            when(adminProductMapper.toAdminProductOptionCreateResponseDto(saved)).thenReturn(responseDto);

            // when
            AdminProductOptionCreateResponseDto result =
                    adminProductOptionService.createProductOption(productId, requestDto);

            // then
            assertThat(result).isSameAs(responseDto);

            verify(productRepository, times(1)).findByIdAndDeletedAtIsNull(productId);
            verify(productOptionRepository, times(1))
                    .existsByProductAndOptionAndDeletedAtIsNull(product, "270");

            verify(productOptionRepository, times(1)).save(productOptionCaptor.capture());
            assertThat(productOptionCaptor.getValue()).isNotNull();

            verify(adminProductMapper, times(1)).toAdminProductOptionCreateResponseDto(saved);
        }

        @Test
        void 등록_시_상품이_존재하지_않으면_예외가_발생하며_저장_로직은_실행되지_않는다() {
            // given
            UUID productId = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.empty());

            AdminProductOptionCreateRequestDto requestDto = mock(AdminProductOptionCreateRequestDto.class);

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.createProductOption(productId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(productOptionRepository, never()).existsByProductAndOptionAndDeletedAtIsNull(any(), anyString());
            verify(productOptionRepository, never()).save(any());
            verify(adminProductMapper, never()).toAdminProductOptionCreateResponseDto(any());
        }

        @Test
        void 이미_존재하는_옵션인_경우_예외가_발생하며_저장_로직은_실행되지_않는다() {
            // given
            UUID productId = UUID.randomUUID();

            Product product = mock(Product.class);
            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));

            AdminProductOptionCreateRequestDto requestDto = mock(AdminProductOptionCreateRequestDto.class);
            when(requestDto.getOption()).thenReturn("280");

            when(productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "280"))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.createProductOption(productId, requestDto))
                    .isInstanceOf(CustomException.class);

            verify(productOptionRepository, times(1))
                    .existsByProductAndOptionAndDeletedAtIsNull(product, "280");

            verify(productOptionRepository, never()).save(any());
            verify(adminProductMapper, never()).toAdminProductOptionCreateResponseDto(any());
        }

        @Test
        void 등록_시_중복_체크는_불필요한_반복_없이_1회만_수행된다() {
            // given
            UUID productId = UUID.randomUUID();

            Product product = mock(Product.class);
            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(product));

            AdminProductOptionCreateRequestDto requestDto = mock(AdminProductOptionCreateRequestDto.class);
            when(requestDto.getOption()).thenReturn("290");

            when(productOptionRepository.existsByProductAndOptionAndDeletedAtIsNull(product, "290"))
                    .thenReturn(false);

            ProductOption saved = mock(ProductOption.class);
            when(productOptionRepository.save(any(ProductOption.class))).thenReturn(saved);

            when(adminProductMapper.toAdminProductOptionCreateResponseDto(saved))
                    .thenReturn(mock(AdminProductOptionCreateResponseDto.class));

            // when
            adminProductOptionService.createProductOption(productId, requestDto);

            // then
            verify(productOptionRepository, times(1))
                    .existsByProductAndOptionAndDeletedAtIsNull(product, "290");
        }
    }

    @Nested
    class 상품_옵션_삭제_테스트 {

        @Test
        void 상품과_옵션이_존재하고_소속이_일치하면_소프트_삭제를_수행한다() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            String deletedBy = "admin@unbox.com";

            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(mock(Product.class)));

            ProductOption option = mock(ProductOption.class);
            Product optionProduct = mock(Product.class);
            when(option.getProduct()).thenReturn(optionProduct);
            when(optionProduct.getId()).thenReturn(productId);

            when(productOptionRepository.findByIdAndDeletedAtIsNull(optionId))
                    .thenReturn(Optional.of(option));

            // when
            adminProductOptionService.deleteProductOption(productId, optionId, deletedBy);

            // then
            verify(productRepository, times(1)).findByIdAndDeletedAtIsNull(productId);
            verify(productOptionRepository, times(1)).findByIdAndDeletedAtIsNull(optionId);
            verify(option, times(1)).softDelete(deletedBy);

            verify(productOptionRepository, never()).save(any());
        }

        @Test
        void 삭제_시_상품이_존재하지_않으면_예외가_발생하며_옵션_조회는_하지_않는다() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();

            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.deleteProductOption(productId, optionId, "x"))
                    .isInstanceOf(CustomException.class);

            verify(productOptionRepository, never()).findByIdAndDeletedAtIsNull(any());
        }

        @Test
        void 삭제_시_옵션이_존재하지_않으면_예외가_발생한다() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();

            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(mock(Product.class)));

            when(productOptionRepository.findByIdAndDeletedAtIsNull(optionId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.deleteProductOption(productId, optionId, "x"))
                    .isInstanceOf(CustomException.class);

            verify(productOptionRepository, times(1)).findByIdAndDeletedAtIsNull(optionId);
            verifyNoMoreInteractions(productOptionRepository);
        }

        @Test
        void 옵션이_다른_상품에_속해_있는_경우_예외가_발생하며_삭제하지_않는다() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();
            UUID otherProductId = UUID.randomUUID();

            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(mock(Product.class)));

            ProductOption option = mock(ProductOption.class);
            Product optionProduct = mock(Product.class);
            when(option.getProduct()).thenReturn(optionProduct);
            when(optionProduct.getId()).thenReturn(otherProductId);

            when(productOptionRepository.findByIdAndDeletedAtIsNull(optionId))
                    .thenReturn(Optional.of(option));

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.deleteProductOption(productId, optionId, "x"))
                    .isInstanceOf(CustomException.class);

            verify(option, never()).softDelete(anyString());
        }

        @Test
        void 옵션에_연결된_상품_정보가_없는_경우_NullPointerException이_발생한다() {
            // given
            UUID productId = UUID.randomUUID();
            UUID optionId = UUID.randomUUID();

            when(productRepository.findByIdAndDeletedAtIsNull(productId))
                    .thenReturn(Optional.of(mock(Product.class)));

            ProductOption option = mock(ProductOption.class);
            when(option.getProduct()).thenReturn(null);

            when(productOptionRepository.findByIdAndDeletedAtIsNull(optionId))
                    .thenReturn(Optional.of(option));

            // when & then
            assertThatThrownBy(() -> adminProductOptionService.deleteProductOption(productId, optionId, "x"))
                    .isInstanceOf(NullPointerException.class);

            verify(option, never()).softDelete(anyString());
        }
    }
}