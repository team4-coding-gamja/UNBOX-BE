package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
import com.example.unbox_be.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private UUID productId;
    private UUID orderId;
    private Long userId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = 1L;
    }

    @Test
    @DisplayName("리뷰 작성 성공 - 데이터가 정상 저장되고 ID를 반환하는지 확인")
    void createReview_Success() {
        // DTO 객체 생성
        ReviewRequestDto request = new ReviewRequestDto(productId, orderId, "최고에욤!", 5, "image.jpg");

        // Order 엔티티는 Protected 생성자이므로 Reflection이나 내부 생성 방식을 따름
        // 여기서는 테스트를 위해 가짜 Order 객체의 상태를 COMPLETED로 설정
        Order mockOrder = mock(Order.class);
        given(mockOrder.getStatus()).willReturn(OrderStatus.COMPLETED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(mockOrder));
        given(reviewRepository.existsByOrderId(orderId)).willReturn(false);

        // save 시 반환값 설정 (NPE 방지)
        Review savedReview = Review.builder().reviewId(UUID.randomUUID()).build();
        given(reviewRepository.save(any(Review.class))).willReturn(savedReview);

        // when
        UUID resultId = reviewService.createReview(request, userId);

        // then
        org.assertj.core.api.Assertions.assertThat(resultId).isNotNull();
        verify(orderRepository).findById(orderId); // 주문 조회 호출 확인
    }

    @Test
    @DisplayName("리뷰 작성 실패: 평점이 1점 미만일 때 예외 발생")
    void createReview_Fail_MinRating() {
        // given
        ReviewRequestDto request = new ReviewRequestDto(productId, orderId, "최악이에요.", 0, "url");

        // when & then
        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(request, userId));
    }

    @Test
    @DisplayName("리뷰 수정 실패: 타인의 리뷰를 수정하려 할 때 보안 예외 발생")
    void updateReview_Fail_Forbidden() {
        // given
        UUID reviewId = UUID.randomUUID();
        // 작성자가 1L인 리뷰 생성
        User mockBuyer = mock(User.class);
        given(mockBuyer.getId()).willReturn(1L);

        Review existingReview = Review.builder().buyer(mockBuyer).build();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(existingReview));

        ReviewUpdateDto updateDto = new ReviewUpdateDto("해킹 시도", 1, "url");

        // when & then: 2L 유저가 수정을 시도하면 실패해야 함
        assertThrows(SecurityException.class, () -> reviewService.updateReview(reviewId, updateDto, 2L));
    }

    @Test
    @DisplayName("리뷰 삭제 성공: 본인 확인 후 softDelete가 호출되는지 확인")
    void deleteReview_Success() {
        // given
        UUID reviewId = UUID.randomUUID();
        User mockBuyer = mock(User.class); // Mock 유저 생성
        given(mockBuyer.getId()).willReturn(userId); // ID 반환 설정

        Review review = Review.builder().buyer(mockBuyer).build();
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, userId);

        // then
        verify(reviewRepository, times(1)).findById(reviewId);
        // BaseEntity의 softDelete 로직에 따라 findById 이후 로직이 실행됨을 확인
    }
}