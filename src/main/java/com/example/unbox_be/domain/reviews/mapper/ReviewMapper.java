package com.example.unbox_be.domain.reviews.mapper;

import com.example.unbox_be.domain.reviews.dto.ReviewResponseDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponseDto toResponseDto(Review review) {
        return ReviewResponseDto.builder()
                .reviewId(review.getReviewId())  // 변경된 필드명 사용
                .orderId(review.getOrder().getId())
                .userName(review.getBuyer().getNickname()) // User 엔티티 필드 확인 필요
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .build();
    }
}