package com.example.unbox_be.domain.reviews.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewCreateRequestDto {

    private String content;
    private Integer rating;
    private String imageUrl;

    private UUID orderId;
}
