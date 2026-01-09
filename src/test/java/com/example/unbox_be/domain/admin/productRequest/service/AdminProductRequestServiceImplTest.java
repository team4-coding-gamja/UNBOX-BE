package com.example.unbox_be.domain.admin.productRequest.service;

import com.example.unbox_be.domain.product.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_be.domain.product.mapper.AdminProductRequestMapper;
import com.example.unbox_be.domain.product.entity.ProductRequest;
import com.example.unbox_be.domain.product.entity.ProductRequestStatus;
import com.example.unbox_be.domain.product.repository.ProductRequestRepository;
import com.example.unbox_be.domain.product.service.AdminProductRequestServiceImpl;
import org.junit.jupiter.api.DisplayName;
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
class AdminProductRequestServiceImplTest {

    @InjectMocks
    private AdminProductRequestServiceImpl adminProductRequestService;

    @Mock private ProductRequestRepository productRequestRepository;
    @Mock private AdminProductRequestMapper adminProductRequestMapper;

    @Test
    @DisplayName("성공: repository.findAll(pageable) 결과를 mapper로 Page.map 변환해서 반환한다")
    void 상품요청목록조회_정상_매핑된페이지를반환한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        ProductRequest pr1 = mock(ProductRequest.class);
        ProductRequest pr2 = mock(ProductRequest.class);

        Page<ProductRequest> repoPage = new PageImpl<>(List.of(pr1, pr2), pageable, 2);

        when(productRequestRepository.findAll(pageable)).thenReturn(repoPage);

        AdminProductRequestListResponseDto dto1 = mock(AdminProductRequestListResponseDto.class);
        AdminProductRequestListResponseDto dto2 = mock(AdminProductRequestListResponseDto.class);
        when(adminProductRequestMapper.toListResponseDto(pr1)).thenReturn(dto1);
        when(adminProductRequestMapper.toListResponseDto(pr2)).thenReturn(dto2);

        // when
        Page<AdminProductRequestListResponseDto> result =
                adminProductRequestService.getProductRequests(pageable);

        // then
        verify(productRequestRepository, times(1)).findAll(pageable);
        verify(adminProductRequestMapper, times(1)).toListResponseDto(pr1);
        verify(adminProductRequestMapper, times(1)).toListResponseDto(pr2);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(dto1, dto2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("성공: 조회 결과가 비어있으면 빈 Page를 반환하고 mapper는 호출되지 않는다")
    void 상품요청목록조회_빈결과_빈페이지반환_매퍼호출없음() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        Page<ProductRequest> empty = Page.empty(pageable);
        when(productRequestRepository.findAll(pageable)).thenReturn(empty);

        // when
        Page<AdminProductRequestListResponseDto> result =
                adminProductRequestService.getProductRequests(pageable);

        // then
        verify(productRequestRepository, times(1)).findAll(pageable);
        verify(adminProductRequestMapper, never()).toListResponseDto(any());

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("성공: totalElements가 content 크기와 달라도 Page의 totalElements를 유지한다")
    void 상품요청목록조회_totalElements유지() {
        // given
        Pageable pageable = PageRequest.of(1, 30);

        ProductRequest pr1 = mock(ProductRequest.class);
        Page<ProductRequest> repoPage = new PageImpl<>(List.of(pr1), pageable, 999);

        when(productRequestRepository.findAll(pageable)).thenReturn(repoPage);

        AdminProductRequestListResponseDto dto1 = mock(AdminProductRequestListResponseDto.class);
        when(adminProductRequestMapper.toListResponseDto(pr1)).thenReturn(dto1);

        // when
        Page<AdminProductRequestListResponseDto> result =
                adminProductRequestService.getProductRequests(pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(999);
        assertThat(result.getContent()).containsExactly(dto1);
    }

    @Test
    @DisplayName("검증: repository에서 받은 ProductRequest 개수만큼 mapper가 정확히 호출된다")
    void 상품요청목록조회_매퍼호출횟수검증() {
        // given
        Pageable pageable = PageRequest.of(0, 50);

        ProductRequest pr1 = mock(ProductRequest.class);
        ProductRequest pr2 = mock(ProductRequest.class);
        ProductRequest pr3 = mock(ProductRequest.class);

        Page<ProductRequest> repoPage = new PageImpl<>(List.of(pr1, pr2, pr3), pageable, 3);
        when(productRequestRepository.findAll(pageable)).thenReturn(repoPage);

        when(adminProductRequestMapper.toListResponseDto(any(ProductRequest.class)))
                .thenReturn(mock(AdminProductRequestListResponseDto.class));

        // when
        Page<AdminProductRequestListResponseDto> result =
                adminProductRequestService.getProductRequests(pageable);

        // then
        verify(adminProductRequestMapper, times(3)).toListResponseDto(any(ProductRequest.class));
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("성공: 요청이 존재하면 updateStatus를 호출하고 mapper 응답을 반환한다")
    void 상품요청상태변경_정상_updateStatus호출_응답반환() {
        // given
        UUID id = UUID.randomUUID();

        ProductRequest productRequest = mock(ProductRequest.class);
        when(productRequestRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(productRequest));

        AdminProductRequestUpdateRequestDto requestDto = mock(AdminProductRequestUpdateRequestDto.class);

        ProductRequestStatus status = ProductRequestStatus.values()[0];
        doReturn(status).when(requestDto).getStatus();

        AdminProductRequestUpdateResponseDto responseDto = mock(AdminProductRequestUpdateResponseDto.class);
        when(adminProductRequestMapper.toUpdateResponseDto(productRequest)).thenReturn(responseDto);

        // when
        AdminProductRequestUpdateResponseDto result =
                adminProductRequestService.updateProductRequestStatus(id, requestDto);

        // then
        assertThat(result).isSameAs(responseDto);

        verify(productRequestRepository, times(1)).findByIdAndDeletedAtIsNull(id);
        verify(productRequest, times(1)).updateStatus(eq(status));
        verify(adminProductRequestMapper, times(1)).toUpdateResponseDto(productRequest);
    }

    @Test
    @DisplayName("실패: 요청이 없으면 IllegalArgumentException 발생, updateStatus/mapper는 호출되지 않는다")
    void 상품요청상태변경_요청없음_예외발생_후속호출없음() {
        // given
        UUID id = UUID.randomUUID();
        when(productRequestRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.empty());

        AdminProductRequestUpdateRequestDto requestDto = mock(AdminProductRequestUpdateRequestDto.class);

        // when & then
        assertThatThrownBy(() -> adminProductRequestService.updateProductRequestStatus(id, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product request not found");

        verify(productRequestRepository, times(1)).findByIdAndDeletedAtIsNull(id);
        verify(adminProductRequestMapper, never()).toUpdateResponseDto(any());
    }

    @Test
    @DisplayName("검증: requestDto.getStatus는 1회 호출된다(불필요한 중복 호출 방지)")
    void 상품요청상태변경_getStatus호출횟수1회() {
        // given
        UUID id = UUID.randomUUID();

        ProductRequest productRequest = mock(ProductRequest.class);
        when(productRequestRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(productRequest));

        AdminProductRequestUpdateRequestDto requestDto = mock(AdminProductRequestUpdateRequestDto.class);
        ProductRequestStatus status = ProductRequestStatus.values()[0];
        doReturn(status).when(requestDto).getStatus();

        when(adminProductRequestMapper.toUpdateResponseDto(productRequest))
                .thenReturn(mock(AdminProductRequestUpdateResponseDto.class));

        // when
        adminProductRequestService.updateProductRequestStatus(id, requestDto);

        // then
        verify(requestDto, times(1)).getStatus();
    }

    @Test
    @DisplayName("검증: mapper는 updateStatus 이후에 호출된다(순서 보장)")
    void 상품요청상태변경_호출순서_updateStatus_후_mapper() {
        // given
        UUID id = UUID.randomUUID();

        ProductRequest productRequest = mock(ProductRequest.class);
        when(productRequestRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(productRequest));

        AdminProductRequestUpdateRequestDto requestDto = mock(AdminProductRequestUpdateRequestDto.class);
        ProductRequestStatus status = ProductRequestStatus.values()[0];
        doReturn(status).when(requestDto).getStatus();

        when(adminProductRequestMapper.toUpdateResponseDto(productRequest))
                .thenReturn(mock(AdminProductRequestUpdateResponseDto.class));

        // when
        adminProductRequestService.updateProductRequestStatus(id, requestDto);

        // then
        InOrder inOrder = inOrder(productRequest, adminProductRequestMapper);
        inOrder.verify(productRequest).updateStatus(any());
        inOrder.verify(adminProductRequestMapper).toUpdateResponseDto(productRequest);
    }

    @Test
    @DisplayName("엣지: requestDto.getStatus가 null이어도 updateStatus는 호출된다(현재 서비스 로직 기준)")
    void 상품요청상태변경_statusNull_호출여부확인() {
        // given
        UUID id = UUID.randomUUID();

        ProductRequest productRequest = mock(ProductRequest.class);
        when(productRequestRepository.findByIdAndDeletedAtIsNull(id))
                .thenReturn(Optional.of(productRequest));

        AdminProductRequestUpdateRequestDto requestDto = mock(AdminProductRequestUpdateRequestDto.class);
        doReturn(null).when(requestDto).getStatus();

        when(adminProductRequestMapper.toUpdateResponseDto(productRequest))
                .thenReturn(mock(AdminProductRequestUpdateResponseDto.class));

        // when
        adminProductRequestService.updateProductRequestStatus(id, requestDto);

        // then
        verify(productRequest, times(1)).updateStatus(any()); // null이어도 any()로 검증
        verify(adminProductRequestMapper, times(1)).toUpdateResponseDto(productRequest);
    }
}

