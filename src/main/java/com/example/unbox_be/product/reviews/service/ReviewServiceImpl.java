package com.example.unbox_be.product.reviews.service;

import com.example.unbox_be.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import com.example.unbox_be.order.adapter.OrderClientAdapter;
import com.example.unbox_be.order.entity.OrderStatus;
import com.example.unbox_be.product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.product.reviews.entity.Review;
import com.example.unbox_be.product.reviews.entity.ReviewProductSnapshot;
import com.example.unbox_be.product.reviews.mapper.ReviewMapper;
import com.example.unbox_be.product.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderClientAdapter orderClientAdapter;
    private final ReviewMapper reviewMapper;

    // ✅ 리뷰 생성
    @Override
    @Transactional
    public ReviewCreateResponseDto createReview(Long userId, ReviewCreateRequestDto requestDto) {

        UUID orderId = requestDto.getOrderId();

        // 1) 중복 작성 방지 (1주문 1리뷰)
        if (reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)) {
            throw new CustomException(ErrorCode.ALREADY_REVIEWED);
        }

        // 2) 주문(리뷰용 정보) 조회
        OrderForReviewInfoResponse orderInfo = orderClientAdapter.getOrderForReview(orderId);

        // 3) 주문 상태 검증
        if (orderInfo.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // 4) 작성 권한 검증 (구매자 본인)
        if (orderInfo.getBuyerId() == null || !orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 5) Review 스냅샷 구성 (OrderSnapshot 기반 복사)
        ReviewProductSnapshot snapshot = ReviewProductSnapshot.builder()
                .buyerId(orderInfo.getBuyerId())
                .buyerNickname(orderInfo.getBuyerNickname())
                .orderStatus(orderInfo.getOrderStatus())
                .productId(orderInfo.getProductId())
                .productName(orderInfo.getProductName())
                .modelNumber(orderInfo.getModelNumber())
                .productImageUrl(orderInfo.getProductImageUrl())
                .productOptionId(orderInfo.getProductOptionId())
                .productOptionName(orderInfo.getProductOptionName())
                .brandName(orderInfo.getBrandName())
                .build();

        // 6) 엔티티 생성
        Review review = Review.createReview(
                orderId,
                requestDto.getContent(),
                requestDto.getRating(),
                requestDto.getReviewImageUrl(),
                snapshot
        );

        Review saved = reviewRepository.save(review);
        return reviewMapper.toReviewCreateResponseDto(saved);
    }

    // ✅ 리뷰 조회
    @Override
    @Transactional(readOnly = true)
    public ReviewDetailResponseDto getReview(UUID reviewId) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toReviewDetailResponseDto(review);
    }

    // ✅ 리뷰 수정
    @Override
    @Transactional
    public ReviewUpdateResponseDto updateReview(Long userId, UUID reviewId, ReviewUpdateRequestDto requestDto) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 권한 검증: review.orderId → 주문 조회 → buyerId 비교
        OrderForReviewInfoResponse orderInfo = orderClientAdapter.getOrderForReview(review.getOrderId());
        if (orderInfo.getBuyerId() == null || !orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.update(requestDto.getContent(), requestDto.getRating(), requestDto.getReviewImageUrl());
        return reviewMapper.toReviewUpdateResponseDto(review);
    }

    // ✅ 리뷰 삭제
    @Override
    @Transactional
    public void deleteReview(Long userId, UUID reviewId, String deletedBy) {
        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 권한 검증
        OrderForReviewInfoResponse orderInfo = orderClientAdapter.getOrderForReview(review.getOrderId());
        if (orderInfo.getBuyerId() == null || !orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.softDelete(deletedBy);
    }
}