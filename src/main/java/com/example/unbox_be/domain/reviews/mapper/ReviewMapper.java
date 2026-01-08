package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewDetailResponseDto toResponseDto(Review review) {
        return ReviewDetailResponseDto.builder()
                .id(review.getReviewId())  // 변경된 필드명 사용
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .order(ReviewDetailResponseDto.OrderInfo.builder()
                        .id(review.getOrder().getId())
                        .price(review.getOrder().getPrice())
                        .buyer(ReviewDetailResponseDto.UserInfo.builder()
                                .id(review.getBuyer().getId())
                                .nickname(review.getBuyer().getNickname())
                                .build())
                        .productOption(ReviewDetailResponseDto.ProductOptionInfo.builder()
                                .id(review.getOrder().getProductOption().getId())
                                .option(review.getOrder().getProductOption().getOption())
                                .product(ReviewDetailResponseDto.ProductInfo.builder()
                                        .id(review.getOrder().getProductOption().getProduct().getId())
                                        .name(review.getOrder().getProductOption().getProduct().getName())
                                        .imageUrl(review.getOrder().getProductOption().getProduct().getImageUrl())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}