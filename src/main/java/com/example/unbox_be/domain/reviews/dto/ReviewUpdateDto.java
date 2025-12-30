package com.example.unbox_be.domain.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 수정용 DTO
@Getter
@NoArgsConstructor  // 기본생성자
@AllArgsConstructor // 테스트코드 작성을 위함
public class ReviewUpdateDto {
    private String content;
    private Integer rating;
    private String imageUrl;
}