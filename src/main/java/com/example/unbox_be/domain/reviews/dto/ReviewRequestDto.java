package com.example.unbox_be.domain.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Getter
@NoArgsConstructor  // 기본생성자
@AllArgsConstructor // 테스트코드를 위함
public class ReviewRequestDto {
    private UUID productId;
    private UUID orderId;
    private String content;
    private Integer rating;
    private String imageUrl;
}