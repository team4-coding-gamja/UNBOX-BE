package com.example.unbox_product.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewCreateResponseDto {

    private UUID reviewId;
    private String content;
    private Integer rating;
    private String reviewImageUrl;
}
