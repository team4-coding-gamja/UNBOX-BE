package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.entity.Review;
//import com.example.unbox_be.domain.reviews.mapper.ReviewMapper;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

//    @Mock
//    private ReviewMapper reviewMapper;

    @Nested
    @DisplayName("리뷰 생성 테스트")
    class CreateReview {

        @Test
        @DisplayName("성공: 모든 조건이 충족되면 리뷰가 정상적으로 생성된다.")
        void createReview_success() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();

            User buyer = mock(User.class);
            given(buyer.getId()).willReturn(userId);

            Product product = mock(Product.class);
            given(product.getId()).willReturn(UUID.randomUUID());

            ProductOption option = mock(ProductOption.class);
            given(option.getProduct()).willReturn(product);

            Order order = mock(Order.class);
            given(order.getBuyer()).willReturn(buyer);
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);
            given(order.getProductOption()).willReturn(option);

            ReviewRequestDto requestDto = new ReviewRequestDto(orderId, "최고예요!", 5, null);

            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(reviewRepository.existsByOrderId(orderId)).willReturn(false);

            // NPE 방지: 저장 후 반환될 Mock 객체 설정
            Review mockReview = Review.builder().reviewId(UUID.randomUUID()).build();
            given(reviewRepository.save(any(Review.class))).willReturn(mockReview);

            // when
            UUID resultId = reviewService.createReview(requestDto, userId);

            // then
            assertThat(resultId).isEqualTo(mockReview.getReviewId());
        }

        @Test
        @DisplayName("실패: 주문 상태가 COMPLETED가 아니면 예외가 발생한다.")
        void createReview_fail_orderNotCompleted() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = mock(Order.class);
            given(order.getStatus()).willReturn(OrderStatus.PENDING_SHIPMENT); // 거래 완료 전 상태

            ReviewRequestDto requestDto = new ReviewRequestDto(orderId, "내용", 5, null);
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.ORDER_CANNOT_BE_CANCELLED.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 해당 주문에 리뷰가 존재하면 예외가 발생한다.")
        void createReview_fail_alreadyReviewed() {
            // given
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();
            Order order = mock(Order.class);

            // 상태 체크는 통과해야 하므로 최소한의 설정만 진행
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);

            // [수정] UnnecessaryStubbing 방지: 예외 발생 시점까지만 설정
            given(reviewRepository.existsByOrderId(orderId)).willReturn(true);

            ReviewRequestDto requestDto = new ReviewRequestDto(orderId, "내용", 5, null);

            // when & then
            assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.ALREADY_REVIEWED.getMessage());
        }

        @Test
        @DisplayName("실패: 평점이 범위를 벗어나면 예외가 발생한다.")
        void createReview_fail_invalidRating() {
            // [given]
            Long userId = 1L;
            UUID orderId = UUID.randomUUID();

            User buyer = mock(User.class);
            given(buyer.getId()).willReturn(userId);

            Order order = mock(Order.class);
            given(order.getBuyer()).willReturn(buyer); // buyer를 찾지 못해 NPE가 나는 것을 방지
            given(order.getStatus()).willReturn(OrderStatus.COMPLETED);

            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(reviewRepository.existsByOrderId(orderId)).willReturn(false);

            // 평점 검증 단계에서 예외가 발생하므로, 그 이후의 'ProductOption' 설정은 불필요 (삭제됨)
            ReviewRequestDto requestDto = new ReviewRequestDto(orderId, "잘못된 평점", 6, null);

            // [when & then]
            assertThatThrownBy(() -> reviewService.createReview(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_RATING.getMessage());

            // 저장 로직까지 도달하지 않았는지 최종 확인
            verify(reviewRepository, never()).save(any(Review.class));
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 테스트")
    class DeleteReview {
        @Test
        @DisplayName("실패: 본인이 작성하지 않은 리뷰를 삭제하려고 하면 예외가 발생한다.")
        void deleteReview_fail_notOwner() {
            // given
            UUID reviewId = UUID.randomUUID();
            Long loginUserId = 1L;
            Long otherUserId = 99L;

            User otherUser = mock(User.class);
            given(otherUser.getId()).willReturn(otherUserId);

            Review review = mock(Review.class);
            given(review.getBuyer()).willReturn(otherUser);

            given(reviewRepository.findByIdAndDeletedAtIsNull(reviewId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewService.deleteReview(reviewId, loginUserId))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_REVIEW_OWNER.getMessage());
        }
    }
}