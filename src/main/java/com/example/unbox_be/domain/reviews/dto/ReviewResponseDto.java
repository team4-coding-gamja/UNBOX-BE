package com.example.unbox_be.domain.reviews.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Builder
public class ReviewResponseDto {
    private UUID reviewId;
    private UUID orderId;
    private String userName;
    private String content;
    private Integer rating;
    private String imageUrl;
    private LocalDateTime createdAt;
}