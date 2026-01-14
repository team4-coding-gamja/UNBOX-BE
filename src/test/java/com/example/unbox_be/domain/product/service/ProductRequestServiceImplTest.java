//package com.example.unbox_be.domain.product.service;
//
//import com.example.unbox_be.domain.product.dto.request.ProductRequestRequestDto;
//import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;
//import com.example.unbox_be.domain.product.entity.ProductRequest;
//import com.example.unbox_be.domain.product.entity.ProductRequestStatus;
//import com.example.unbox_be.domain.product.mapper.ProductRequestMapper;
//import com.example.unbox_be.domain.product.repository.ProductRequestRepository;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.domain.user.repository.UserRepository;
//import com.example.unbox_be.global.error.exception.CustomException;
//import com.example.unbox_be.global.error.exception.ErrorCode;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class ProductRequestServiceImplTest {
//
//    @InjectMocks
//    private ProductRequestServiceImpl productRequestService;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private ProductRequestRepository productRequestRepository;
//
//    @Mock
//    private ProductRequestMapper productRequestMapper;
//
//    @Test
//    @DisplayName("상품 등록 요청 성공")
//    void createProductRequest_Success() {
//        // given
//        Long userId = 1L;
//        ProductRequestRequestDto requestDto = new ProductRequestRequestDto("New Product", "New Brand");
//
//        // User 생성 (정적 팩토리 메서드 사용)
//        User user = User.createUser("test@test.com", "password", "testuser", "01012345678");
//        ReflectionTestUtils.setField(user, "id", userId);
//
//        ProductRequest productRequest = ProductRequest.createProductRequest(userId, "New Product", "New Brand");
//        ProductRequestResponseDto responseDto = ProductRequestResponseDto.builder()
//                .id(productRequest.getId())
//                .name("New Product")
//                .brandName("New Brand")
//                .status(ProductRequestStatus.PENDING)
//                .build();
//
//        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
//        given(productRequestRepository.save(any(ProductRequest.class))).willReturn(productRequest);
//        given(productRequestMapper.toProductRequestResponseDto(any(ProductRequest.class))).willReturn(responseDto);
//
//        // when
//        ProductRequestResponseDto result = productRequestService.createProductRequest(userId, requestDto);
//
//        // then
//        assertThat(result.getName()).isEqualTo("New Product");
//        assertThat(result.getBrandName()).isEqualTo("New Brand");
//        assertThat(result.getStatus()).isEqualTo(ProductRequestStatus.PENDING);
//
//        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
//        verify(productRequestRepository).save(any(ProductRequest.class));
//    }
//
//    @Test
//    @DisplayName("상품 등록 요청 실패 - 사용자 없음")
//    void createProductRequest_Fail_UserNotFound() {
//        // given
//        Long userId = 1L;
//        ProductRequestRequestDto requestDto = new ProductRequestRequestDto("New Product", "New Brand");
//
//        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> productRequestService.createProductRequest(userId, requestDto))
//                .isInstanceOf(CustomException.class)
//                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
//    }
//}
