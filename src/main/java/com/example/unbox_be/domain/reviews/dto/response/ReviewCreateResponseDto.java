package com.example.unbox_be.domain.reviews.dto.response;

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

    private UUID id;
    private String content;
    private Integer rating;
    private String imageUrl;
}