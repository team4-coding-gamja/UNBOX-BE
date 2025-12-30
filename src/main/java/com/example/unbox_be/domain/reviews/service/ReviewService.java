package com.example.unbox_be.domain.reviews.service;

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

    // 리뷰 생성
    @Transactional
    public UUID createReview(ReviewRequestDto requestDto, Long userId) {

        // 1. [요구사항] 주문 및 배송 완료 여부 확인
        // TODO: OrderService 또는 OrderRepository를 통해 주문 상태가 'DELIVERED'인지 확인하는 로직 필요
        boolean isDelivered = true; // 우선 검증 통과로 가정
        if (!isDelivered) {
            throw new IllegalArgumentException("배송이 완료된 주문만 리뷰를 작성할 수 있습니다.");
        }

        // 2. 주문당 리뷰 중복 작성 방지
        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new IllegalStateException("이미 해당 주문에 대한 리뷰를 작성했습니다.");
        }

        // 3. [요구사항] 평점 1~5점 제한
        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            throw new IllegalArgumentException("평점은 1점에서 5점 사이여야 합니다.");
        }

        // 4. 엔티티 빌드 및 저장
        Review review = Review.builder()
                .productId(requestDto.getProductId())
                .orderId(requestDto.getOrderId())
                .buyerId(userId)
                .content(requestDto.getContent())
                .rating(requestDto.getRating())
                .imageUrl(requestDto.getImageUrl())
                .build();

        return reviewRepository.save(review).getReviewId();
    }

    public Page<Review> getReviewsByProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findAllByProductIdAndDeletedAtIsNull(productId, pageable);
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
    public void deleteReview(UUID reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        review.delete(userId);
    }


    // 상품 PK로 삭제되지 않은 리뷰 리스트를 페이징 조회
    public Page<Review> getReviewsByProductId(UUID productId, Pageable pageable) {
        return reviewRepository.findAllByProductIdAndDeletedAtIsNull(productId, pageable);
    }
}