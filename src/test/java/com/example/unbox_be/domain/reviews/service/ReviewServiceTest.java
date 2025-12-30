package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 성공 - 데이터가 정상인 경우")
    void createReview_Success() {
        // Given: 테스트 환경 구축 (DTO 생성자가 이제 5개 인수를 정상적으로 받습니다)
        ReviewRequestDto request = new ReviewRequestDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "정말 만족합니다!",
                5,
                "url"
        );

        // Mock 설정: 중복 주문 없음
        given(reviewRepository.existsByOrderId(any(UUID.class))).willReturn(false);

        // When: 로직 실행
        reviewService.createReview(request, 1L);

        // Then: save 메서드가 호출되었는지 검증
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 평점이 1점 미만인 경우 예외 발생")
    void createReview_Fail_RatingTooLow() {
        ReviewRequestDto request = new ReviewRequestDto(
                UUID.randomUUID(), UUID.randomUUID(), "별로에요", 0, "url"
        );

        assertThrows(IllegalArgumentException.class, () -> {
            reviewService.createReview(request, 1L);
        });
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 타인이 수정을 시도할 때")
    void updateReview_Fail_Forbidden() {
        UUID reviewId = UUID.randomUUID();
        Review existingReview = Review.builder()
                .buyerId(1L)
                .build();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(existingReview));

        ReviewUpdateDto updateDto = new ReviewUpdateDto("수정 시도", 4, "url");

        assertThrows(SecurityException.class, () -> {
            reviewService.updateReview(reviewId, updateDto, 2L);
        });
    }
}