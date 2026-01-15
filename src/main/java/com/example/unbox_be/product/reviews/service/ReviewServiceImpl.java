package com.example.unbox_be.product.reviews.service;

import com.example.unbox_be.order.entity.OrderStatus;
import com.example.unbox_be.order.repository.OrderRepository;
import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.product.reviews.entity.Review;
import com.example.unbox_be.product.reviews.mapper.ReviewMapper;
import com.example.unbox_be.product.reviews.repository.ReviewRepository;
import com.example.unbox_be.user.user.entity.User;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;

    /**
     * [리뷰 생성]
     * 비즈니스 규칙:
     * 1. 주문 상태가 반드시 'COMPLETED'여야 함.
     * 2. 주문 1개당 리뷰는 오직 1개만 작성 가능 (1:1 관계).
     * 3. 주문한 구매자 본인만 리뷰 작성 가능.
     * 4. 평점은 반드시 1점 ~ 5점 사이여야 함.
     */
    // ✅ 리뷰 생성
    @Transactional
    public ReviewCreateResponseDto createReview(Long userId, ReviewCreateRequestDto requestDto) {

        // 1) 주문 조회
        Order order = orderRepository.findByIdAndDeletedAtIsNull(requestDto.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2) 주문 상태 검증 (거래 완료 상태인지 확인)
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // 3) 리뷰 중복 작성 검증 (1주문 1리뷰)
        if (reviewRepository.existsByOrderIdAndDeletedAtIsNull(requestDto.getOrderId())) {
            throw new CustomException(ErrorCode.ALREADY_REVIEWED);
        }

        // 4) 작성 권한 검증 (로그인 유저가 실제 구매자인지)
        User buyer = order.getBuyer();
        if (buyer == null || !buyer.getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 5) 주문에서 상품 추출 (Order 내 스냅샷/ID 존재 여부로 대체 가능 - nullable=false이므로 생략 가능)
        // Order에 productId, productOptionId가 @Column(nullable=false)로 보장됨.

        // 6) Review 생성 (엔티티 내부 검증도 같이 타게)
        Review review = Review.createReview(
                order,
                requestDto.getContent(),
                requestDto.getRating(),
                requestDto.getImageUrl()
        );

        // 7) 저장
        Review savedReview = reviewRepository.save(review);

        return reviewMapper.toReviewCreateResponseDto(savedReview);
    }


    // ✅ 리뷰 조회
    @Transactional(readOnly = true)
    public ReviewDetailResponseDto getReview(UUID reviewId) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toReviewDetailResponseDto(review);
    }

    // ✅ 리뷰 수정
    @Transactional
    public ReviewUpdateResponseDto updateReview(Long userId, UUID reviewId, ReviewUpdateRequestDto requestDto) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getOrder().getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.update(requestDto.getContent(), requestDto.getRating(), requestDto.getImageUrl());
        return reviewMapper.toReviewUpdateResponseDto(review);
    }

    // ✅ 리뷰 삭제
    @Transactional
    public void deleteReview(Long userId, UUID reviewId, String deletedBy) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getOrder().getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.softDelete(deletedBy);
    }
}