package com.example.unbox_be.domain.reviews.service;

import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.domain.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.mapper.ReviewMapper;
import com.example.unbox_be.domain.reviews.repository.ReviewRepository;
import com.example.unbox_be.global.client.order.OrderClient;
import com.example.unbox_be.global.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_be.global.client.product.ProductClient;
import com.example.unbox_be.global.client.product.dto.ProductOptionInfoResponse;
import com.example.unbox_be.global.client.user.UserClient;
import com.example.unbox_be.global.client.user.dto.UserForReviewInfoResponse;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    private final OrderClient orderClient;
    private final UserClient userClient;
    private final ProductClient productClient;

    // ✅ 리뷰 생성
    @Transactional
    public ReviewCreateResponseDto createReview(Long userId, ReviewCreateRequestDto requestDto) {

        UUID orderId = requestDto.getOrderId();

        // 1) 주문 정보 조회
        OrderForReviewInfoResponse orderInfo = orderClient.getOrderInfo(orderId);
        if (orderInfo == null) throw new CustomException(ErrorCode.ORDER_NOT_FOUND);

        // 2) 주문 상태 검증
        if (orderInfo.getStatus() != OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // 3) 중복 리뷰 체크
        if (reviewRepository.existsByOrderIdAndDeletedAtIsNull(orderId)) {
            throw new CustomException(ErrorCode.ALREADY_REVIEWED);
        }

        // 4) 구매자 검증
        if (!orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 5) 필요한 외부 정보 조회 (로컬 변수로 분리)
        UserForReviewInfoResponse buyerInfo = userClient.getUserInfo(orderInfo.getBuyerId());
        ProductOptionInfoResponse optionInfo = productClient.getProductOption(orderInfo.getProductOptionId());

        // 6) 스냅샷 값 추출 (가독성/안정성 ↑)
        BigDecimal price = orderInfo.getPrice();
        Long buyerId = orderInfo.getBuyerId();
        String buyerNickname = buyerInfo.getNickname();

        UUID productId = optionInfo.getProductId();
        String productName = optionInfo.getProductName();
        String productImageUrl = optionInfo.getImageUrl();

        UUID productOptionId = optionInfo.getProductOptionId();
        String productOptionName = optionInfo.getOptionName();

        // 7) Review 생성 (스냅샷 포함)
        Review review = Review.createReview(
                orderId,
                requestDto.getContent(),
                requestDto.getRating(),
                requestDto.getImageUrl(),
                price,
                buyerId,
                buyerNickname,
                productId,
                productName,
                productImageUrl,
                productOptionId,
                productOptionName
        );

        reviewRepository.save(review);
        return reviewMapper.toReviewCreateResponseDto(review);
    }

    // ✅ 리뷰 상세 조회 (클라이언트 호출로 조립)
    public ReviewDetailResponseDto getReview(Long userId, UUID reviewId) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        OrderForReviewInfoResponse orderInfo = orderClient.getOrderInfo(review.getOrderId());
        if (orderInfo == null) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }

        // (선택) 권한 체크가 필요하면 여기서
        // if (!orderInfo.getBuyerId().equals(userId)) throw ...

        UserForReviewInfoResponse buyerInfo = userClient.getUserInfo(orderInfo.getBuyerId());
        ProductOptionInfoResponse optionInfo = productClient.getProductOption(orderInfo.getProductOptionId());

        // ✅ ReviewDetailResponseDto 조립
        ReviewDetailResponseDto.UserInfo buyer = ReviewDetailResponseDto.UserInfo.builder()
                .id(buyerInfo.getId())
                .nickname(buyerInfo.getNickname())
                .build();

        ReviewDetailResponseDto.ProductInfo product = ReviewDetailResponseDto.ProductInfo.builder()
                .id(optionInfo.getProductId())
                .name(optionInfo.getProductName())
                .imageUrl(optionInfo.getImageUrl())
                .build();

        ReviewDetailResponseDto.ProductOptionInfo productOption = ReviewDetailResponseDto.ProductOptionInfo.builder()
                .id(optionInfo.getProductOptionId())
                .option(optionInfo.getOptionName())
                .product(product)
                .build();

        ReviewDetailResponseDto.OrderInfo order = ReviewDetailResponseDto.OrderInfo.builder()
                .id(orderInfo.getId())
                .price(orderInfo.getPrice())
                .buyer(buyer)
                .productOption(productOption)
                .build();

        return ReviewDetailResponseDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .order(order)
                .build();
    }

    // ✅ 리뷰 수정
    @Transactional
    public ReviewUpdateResponseDto updateReview(Long userId, UUID reviewId, ReviewUpdateRequestDto requestDto) {

        Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 소유자 검증: Review는 orderId만 있으므로 orderClient로 검증
        OrderForReviewInfoResponse orderInfo = orderClient.getOrderInfo(review.getOrderId());
        if (orderInfo == null) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!orderInfo.getBuyerId().equals(userId)) {
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

        OrderForReviewInfoResponse orderInfo = orderClient.getOrderInfo(review.getOrderId());
        if (orderInfo == null) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }

        review.softDelete(deletedBy);
    }
}