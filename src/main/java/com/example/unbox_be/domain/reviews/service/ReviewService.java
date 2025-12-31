package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository; // 실제구현 : order랑 연동

    // 리뷰 생성
    @Transactional
    public UUID createReview(ReviewRequestDto requestDto, Long userId) {

        // 1. 실제 주문 정보를 DB에서 조회 (Order의 PK 타입인 UUID 사용)
        Order order = orderRepository.findById(requestDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        // 2. 주문 상태 검증: COMPLETED 상태여야만 리뷰 작성 가능
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("거래가 완료(COMPLETED)된 주문만 리뷰를 작성할 수 있습니다.");
        }

        // 2. 주문당 리뷰 중복 작성 방지 (중요: 데이터 무결성)
        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new IllegalStateException("이미 해당 주문에 대한 리뷰를 작성했습니다.");
        }

        // 3. [요구사항] 평점 1~5점 제한
        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            throw new IllegalArgumentException("평점은 1점에서 5점 사이여야 합니다.");
        }

        // Builder 대신 엔티티 내 정의한 정적 메서드 사용
        Review review = Review.createReview(
                requestDto.getProductId(),
                requestDto.getOrderId(),
                userId,
                requestDto.getContent(),
                requestDto.getRating(),
                requestDto.getImageUrl()
        );

        return reviewRepository.save(review).getReviewId();
    }

    @Transactional
    public void updateReview(UUID reviewId, ReviewUpdateDto dto, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // [추가] 작성자 본인 확인 로직
        if (!review.getBuyerId().equals(userId)) {
            throw new SecurityException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }

        review.update(dto.getContent(), dto.getRating(), dto.getImageUrl());
    }

    @Transactional
    public void deleteReview(UUID reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getBuyerId().equals(userId)) {
            throw new SecurityException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        // BaseEntity의 softDelete 메서드 호출
        review.softDelete(String.valueOf(userId));
    }

    // 상품별 리뷰 목록 조회
    // 삭제되지 않은 리뷰만 페이징 처리하여 반환합니다.
    public Page<Review> getReviewsByProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findAllByProductIdAndDeletedAtIsNull(productId, pageable);
    }
}