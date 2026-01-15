package com.example.unbox_be.product.reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewUpdateResponseDto {

    private UUID id;
    private String content;
    private Integer rating;
    private String reviewImageUrl;
}
