package com.example.unbox_be.domain.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public class ReviewUpdateDto {
    private String content;
    private Integer rating;
    private String imageUrl;
}