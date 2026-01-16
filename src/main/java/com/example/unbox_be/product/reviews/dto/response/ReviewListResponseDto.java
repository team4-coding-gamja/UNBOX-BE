package com.example.unbox_be.product.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewListResponseDto {

    private UUID reviewId;
    private String content;
    private Integer rating;
    private String reviewImageUrl;
    private LocalDateTime createdAt;
    private String buyerNickname;
    private String productName;
    private String productOptionName;
}
