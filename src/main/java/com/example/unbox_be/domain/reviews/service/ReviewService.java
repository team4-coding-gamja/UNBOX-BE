package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.product.service.ProductService;
import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewResponseDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.mapper.ReviewMapper;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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
    private final OrderRepository orderRepository;
    private final ReviewMapper reviewMapper;
    private final ProductService productService;

    /**
     * [리뷰 생성]
     * 비즈니스 규칙:
     * 1. 주문 상태가 반드시 'COMPLETED'여야 함.
     * 2. 주문 1개당 리뷰는 오직 1개만 작성 가능 (1:1 관계).
     * 3. 주문한 구매자 본인만 리뷰 작성 가능.
     * 4. 평점은 반드시 1점 ~ 5점 사이여야 함.
     */
    @Transactional
    public UUID createReview(ReviewRequestDto requestDto, Long userId) {
        // 1. 주문 존재 여부 확인
        Order order = orderRepository.findByIdAndDeletedAtIsNull(requestDto.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 상태 검증 (거래 완료 상태인지 확인)
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // 3. 리뷰 중복 작성 검증 (해당 주문에 이미 리뷰가 있는지 확인)
        if (reviewRepository.existsByOrderId(requestDto.getOrderId())) {
            throw new CustomException(ErrorCode.ALREADY_REVIEWED);
        }

        // 4. 작성 권한 검증 (로그인한 유저가 실제 구매자인지 확인)
        User buyer = getBuyerOrThrow(order);
        if (!buyer.getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 5. 평점 유효성 검증 (1~5점 범위 체크)
        if (requestDto.getRating() < 1 || requestDto.getRating() > 5) {
            throw new CustomException(ErrorCode.INVALID_RATING);
        }

        // 6. 상품 정보 추출 (주문서의 연관 관계를 통해 Product ID 획득)
        UUID productId = getProductIdFromOrder(order);

        // 7. 리뷰 엔티티 생성 및 DB 저장
        Review review = Review.createReview(
                productId,
                order,
                buyer,
                requestDto.getContent(),
                requestDto.getRating(),
                requestDto.getImageUrl()
        );

        reviewRepository.save(review);
        productService.addReviewData(productId, requestDto.getRating());

        return review.getId();
    }

    /**
     * [상품별 리뷰 목록 조회]
     * 삭제되지 않은 리뷰들을 최신순으로 페이징하여 반환합니다.
     */
    public Page<ReviewResponseDto> getReviewsByProduct(UUID productId, Pageable pageable) {
        return reviewRepository.findAllByProductIdAndDeletedAtIsNull(productId, pageable)
                .map(reviewMapper::toResponseDto);
    }

    /**
     * [리뷰 수정]
     * 작성자 본인 확인 후 내용, 평점, 이미지를 수정합니다.
     */
    @Transactional
    public void updateReview(UUID reviewId, ReviewUpdateDto dto, Long userId) {
        Review review = findReviewOrThrow(reviewId);

        if (!review.getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }
        Integer oldScore = review.getRating();
        // 수정 시에도 평점 범위 검증이 필요할 경우 추가 가능
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new CustomException(ErrorCode.INVALID_RATING);
        }

        review.update(dto.getContent(), dto.getRating(), dto.getImageUrl());
        productService.updateReviewData(review.getProductId(), oldScore, dto.getRating());
    }

    /**
     * [리뷰 삭제]
     * 작성자 본인 확인 후 Soft Delete 처리를 수행합니다.
     */
    @Transactional
    public void deleteReview(UUID reviewId, Long userId) {
        Review review = findReviewOrThrow(reviewId);

        if (!review.getBuyer().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }
        productService.deleteReviewData(review.getProductId(), review.getRating());
        review.softDelete(String.valueOf(userId));
    }

    // --- Private Helper Methods (내부 보조 메서드) ---

    private User getBuyerOrThrow(Order order) {
        User buyer = order.getBuyer();
        if (buyer == null) throw new CustomException(ErrorCode.USER_NOT_FOUND);
        return buyer;
    }

    private UUID getProductIdFromOrder(Order order) {
        // Order -> ProductOption -> Product 연관 구조를 통한 ID 추출로 무결성 보장
        if (order.getProductOption() == null || order.getProductOption().getProduct() == null) {
            throw new CustomException(ErrorCode.DATA_INTEGRITY_ERROR);
        }
        return order.getProductOption().getProduct().getId();
    }

    private Review findReviewOrThrow(UUID reviewId) {
        return reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
    }
}