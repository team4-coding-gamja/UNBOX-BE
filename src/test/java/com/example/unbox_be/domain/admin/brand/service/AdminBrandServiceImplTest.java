package com.example.unbox_be.domain.admin.brand.service;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_be.domain.admin.brand.mapper.AdminBrandMapper;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.repository.BrandRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBrandServiceImplTest {

    @Mock BrandRepository brandRepository;
    @Mock AdminBrandMapper adminBrandMapper;
    @InjectMocks AdminBrandServiceImpl adminBrandService;

    // =========================
    // ✅ 브랜드 목록 조회 getBrands
    // =========================

    @Test
    void 브랜드목록조회_키워드가없으면_findAllByDeletedAtIsNull을_호출하고_매핑된페이지를_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Brand brand1 = mock(Brand.class);
        Brand brand2 = mock(Brand.class);

        Page<Brand> brandPage = new PageImpl<>(List.of(brand1, brand2), pageable, 2);

        AdminBrandListResponseDto dto1 = mock(AdminBrandListResponseDto.class);
        AdminBrandListResponseDto dto2 = mock(AdminBrandListResponseDto.class);

        when(brandRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(brandPage);
        when(adminBrandMapper.toAdminBrandListResponseDto(brand1)).thenReturn(dto1);
        when(adminBrandMapper.toAdminBrandListResponseDto(brand2)).thenReturn(dto2);

        // when
        Page<AdminBrandListResponseDto> result = adminBrandService.getBrands(null, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).containsExactly(dto1, dto2);

        verify(brandRepository, never()).findAll(any(Pageable.class));
        verify(brandRepository, never()).searchByNameAndDeletedAtIsNull(anyString(), any());
    }

    @Test
    void 브랜드목록조회_키워드가있으면_searchByNameAndDeletedAtIsNull을_호출한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Brand brand = mock(Brand.class);
        Page<Brand> brandPage = new PageImpl<>(List.of(brand), pageable, 1);

        AdminBrandListResponseDto mapped = mock(AdminBrandListResponseDto.class);

        when(brandRepository.searchByNameAndDeletedAtIsNull("nike", pageable)).thenReturn(brandPage);
        when(adminBrandMapper.toAdminBrandListResponseDto(brand)).thenReturn(mapped);

        // when
        Page<AdminBrandListResponseDto> result = adminBrandService.getBrands("  nike  ", pageable);

        // then
        assertThat(result.getContent()).containsExactly(mapped);
        verify(brandRepository).searchByNameAndDeletedAtIsNull("nike", pageable);
        verify(brandRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void 브랜드목록조회_키워드가공백이면_findAllByDeletedAtIsNull을_호출하고_매핑된페이지를_반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Brand brand1 = mock(Brand.class);
        Page<Brand> brandPage = new PageImpl<>(List.of(brand1), pageable, 1);

        AdminBrandListResponseDto dto1 = mock(AdminBrandListResponseDto.class);

        when(brandRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(brandPage);
        when(adminBrandMapper.toAdminBrandListResponseDto(brand1)).thenReturn(dto1);

        // when
        Page<AdminBrandListResponseDto> result = adminBrandService.getBrands("    ", pageable);

        // then
        assertThat(result.getContent()).containsExactly(dto1);

        verify(brandRepository).findAllByDeletedAtIsNull(pageable);
        verify(brandRepository, never()).searchByNameAndDeletedAtIsNull(anyString(), any());
    }

    @Test
    void 브랜드수정_이름이null이면_중복검사를하지않고_로고만수정한다() {
        // given
        UUID brandId = UUID.randomUUID();

        AdminBrandUpdateRequestDto dto = AdminBrandUpdateRequestDto.builder()
                .name(null)
                .logoUrl("http://onlyLogo")
                .build();

        Brand brand = mock(Brand.class);
        when(brandRepository.findByIdAndDeletedAtIsNull(brandId)).thenReturn(Optional.of(brand));

        AdminBrandUpdateResponseDto response = mock(AdminBrandUpdateResponseDto.class);
        when(adminBrandMapper.toAdminBrandUpdateResponseDto(brand)).thenReturn(response);

        // when
        AdminBrandUpdateResponseDto result = adminBrandService.updateBrand(brandId, dto);

        // then
        assertThat(result).isSameAs(response);

        verify(brandRepository, never()).existsByNameAndIdNotAndDeletedAtIsNull(anyString(), any());
        verify(brand, never()).updateName(anyString());
        verify(brand).updateLogoUrl("http://onlyLogo");
        verify(adminBrandMapper).toAdminBrandUpdateResponseDto(brand);
    }

    @Test
    void 브랜드수정_로고가null이면_이름만수정한다() {
        // given
        UUID brandId = UUID.randomUUID();

        AdminBrandUpdateRequestDto dto = AdminBrandUpdateRequestDto.builder()
                .name("  NEW  ")
                .logoUrl(null)
                .build();

        Brand brand = mock(Brand.class);

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNotAndDeletedAtIsNull("NEW", brandId)).thenReturn(false);

        AdminBrandUpdateResponseDto response = mock(AdminBrandUpdateResponseDto.class);
        when(adminBrandMapper.toAdminBrandUpdateResponseDto(brand)).thenReturn(response);

        // when
        AdminBrandUpdateResponseDto result = adminBrandService.updateBrand(brandId, dto);

        // then
        assertThat(result).isSameAs(response);

        verify(brand).updateName("NEW");
        verify(brand, never()).updateLogoUrl(anyString());
        verify(adminBrandMapper).toAdminBrandUpdateResponseDto(brand);
    }

    // =========================
    // ✅ 브랜드 상세 조회 getBrandDetail
    // =========================

    @Test
    void 브랜드상세조회_브랜드가없으면_BRAND_NOT_FOUND_예외발생() {
        // given
        UUID brandId = UUID.randomUUID();
        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminBrandService.getBrandDetail(brandId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
                });

        verify(adminBrandMapper, never()).toAdminBrandDetailResponseDto(any());
    }

    @Test
    void 브랜드상세조회_정상요청이면_조회하고_응답을반환한다() {
        // given
        UUID brandId = UUID.randomUUID();

        Brand brand = mock(Brand.class);
        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.of(brand));

        AdminBrandDetailResponseDto responseDto = mock(AdminBrandDetailResponseDto.class);
        when(adminBrandMapper.toAdminBrandDetailResponseDto(brand))
                .thenReturn(responseDto);

        // when
        AdminBrandDetailResponseDto result = adminBrandService.getBrandDetail(brandId);

        // then
        assertThat(result).isSameAs(responseDto);
        verify(adminBrandMapper).toAdminBrandDetailResponseDto(brand);
    }

    // =========================
    // ✅ 브랜드 등록 createBrand
    // =========================

    @Test
    void 브랜드등록_이름이_이미_존재하면_BRAND_ALREADY_EXISTS_예외발생() {
        // given
        AdminBrandCreateRequestDto dto = AdminBrandCreateRequestDto.builder()
                .name("NIKE")
                .logoUrl("http://newLogo")
                .build();

        when(brandRepository.existsByNameAndDeletedAtIsNull("NIKE"))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminBrandService.createBrand(dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.BRAND_ALREADY_EXISTS);
                });

        verify(brandRepository, never()).save(any());
        verify(adminBrandMapper, never()).toAdminBrandCreateResponseDto(any());
    }

    @Test
    void 브랜드등록_정상요청이면_저장하고_응답을반환한다() {
        // given
        AdminBrandCreateRequestDto dto = AdminBrandCreateRequestDto.builder()
                .name("NIKE")
                .logoUrl("http://newLogo")
                .build();

        when(brandRepository.existsByNameAndDeletedAtIsNull("NIKE")).thenReturn(false);

        Brand savedBrand = mock(Brand.class);
        AdminBrandCreateResponseDto response = mock(AdminBrandCreateResponseDto.class);

        when(brandRepository.save(any(Brand.class))).thenReturn(savedBrand);
        when(adminBrandMapper.toAdminBrandCreateResponseDto(savedBrand)).thenReturn(response);

        // when
        AdminBrandCreateResponseDto result = adminBrandService.createBrand(dto);

        // then
        assertThat(result).isSameAs(response);
        verify(brandRepository).save(any(Brand.class));
        verify(adminBrandMapper).toAdminBrandCreateResponseDto(savedBrand);
    }

    // =========================
    // ✅ 브랜드 수정 updateBrand
    // =========================

    @Test
    void 브랜드수정_브랜드가없으면_BRAND_NOT_FOUND_예외발생() {
        // given
        UUID brandId = UUID.randomUUID();
        AdminBrandUpdateRequestDto dto = AdminBrandUpdateRequestDto.builder()
                .name("NEW")
                .logoUrl("http://newLogo")
                .build();

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminBrandService.updateBrand(brandId, dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
                });
    }

    @Test
    void 브랜드수정_이름중복이면_BRAND_ALREADY_EXISTS_예외발생() {
        // given
        UUID brandId = UUID.randomUUID();

        AdminBrandUpdateRequestDto dto = AdminBrandUpdateRequestDto.builder()
                .name("  NEW  ")
                .build();

        Brand brand = mock(Brand.class);

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNotAndDeletedAtIsNull("NEW", brandId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminBrandService.updateBrand(brandId, dto))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.BRAND_ALREADY_EXISTS);
                });

        verify(brand, never()).updateName(anyString());
    }

    @Test
    void 브랜드수정_정상요청이면_이름과로고를수정하고_응답을반환한다() {
        // given
        UUID brandId = UUID.randomUUID();

        AdminBrandUpdateRequestDto dto = AdminBrandUpdateRequestDto.builder()
                .name("  NEW  ")
                .logoUrl("http://newLogo")
                .build();

        Brand brand = mock(Brand.class);

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsByNameAndIdNotAndDeletedAtIsNull("NEW", brandId)).thenReturn(false);

        AdminBrandUpdateResponseDto response = mock(AdminBrandUpdateResponseDto.class);
        when(adminBrandMapper.toAdminBrandUpdateResponseDto(brand)).thenReturn(response);

        // when
        AdminBrandUpdateResponseDto result = adminBrandService.updateBrand(brandId, dto);

        // then
        assertThat(result).isSameAs(response);

        verify(brand).updateName("NEW");          // trim 적용 검증
        verify(brand).updateLogoUrl("http://newLogo");   // 로고 변경 검증
        verify(adminBrandMapper).toAdminBrandUpdateResponseDto(brand);
    }

    // =========================
    // ✅ 브랜드 삭제 deleteBrand
    // =========================

    @Test
    void 브랜드삭제_브랜드가없으면_BRAND_NOT_FOUND_예외발생() {
        // given
        UUID brandId = UUID.randomUUID();
        when(brandRepository.findByIdAndDeletedAtIsNull(brandId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminBrandService.deleteBrand(brandId, "admin@test.com"))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
                });
    }

    @Test
    void 브랜드삭제_정상요청이면_softDelete가호출된다() {
        // given
        UUID brandId = UUID.randomUUID();
        Brand brand = mock(Brand.class);

        when(brandRepository.findByIdAndDeletedAtIsNull(brandId)).thenReturn(Optional.of(brand));

        // when
        adminBrandService.deleteBrand(brandId, "admin@test.com");

        // then
        verify(brand).softDelete("admin@test.com");
    }
}
