package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    /**
     * 리뷰 생성 응답 DTO 변환
     */
    public ReviewCreateResponseDto toReviewCreateResponseDto(Review review) {
        return ReviewCreateResponseDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .build();
    }

    /**
     * 리뷰 수정 응답 DTO 변환
     */
    public ReviewUpdateResponseDto toReviewUpdateResponseDto(Review review) {
        return ReviewUpdateResponseDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .build();
    }

    /**
     * 리뷰 상세 응답 DTO 변환 (스냅샷 데이터 포함)
     */
    public ReviewDetailResponseDto toReviewDetailResponseDto(Review review) {
        return ReviewDetailResponseDto.builder()
                .id(review.getId())
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .order(buildOrderInfo(review))
                .build();
    }

    /**
     * Review 스냅샷 -> OrderInfo 변환
     */
    private ReviewDetailResponseDto.OrderInfo buildOrderInfo(Review review) {
        return ReviewDetailResponseDto.OrderInfo.builder()
                .id(review.getOrderId())
                .price(review.getOrderPrice())
                .buyer(buildBuyerInfo(review))
                .productOption(buildProductOptionInfo(review))
                .build();
    }

    /**
     * Review 스냅샷 -> UserInfo (구매자) 변환
     */
    private ReviewDetailResponseDto.UserInfo buildBuyerInfo(Review review) {
        return ReviewDetailResponseDto.UserInfo.builder()
                .id(review.getBuyerId())
                .nickname(review.getBuyerNickname())
                .build();
    }

    /**
     * Review 스냅샷 -> ProductOptionInfo 변환
     */
    private ReviewDetailResponseDto.ProductOptionInfo buildProductOptionInfo(Review review) {
        return ReviewDetailResponseDto.ProductOptionInfo.builder()
                .id(review.getProductOptionId())
                .option(review.getProductOptionName())
                .product(buildProductInfo(review))
                .build();
    }

    /**
     * Review 스냅샷 -> ProductInfo 변환
     */
    private ReviewDetailResponseDto.ProductInfo buildProductInfo(Review review) {
        return ReviewDetailResponseDto.ProductInfo.builder()
                .id(review.getProductId())
                .name(review.getProductName())
                .imageUrl(review.getProductImageUrl())
                .build();
    }
}