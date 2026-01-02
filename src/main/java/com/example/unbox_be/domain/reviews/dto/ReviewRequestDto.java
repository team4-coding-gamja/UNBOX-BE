package com.example.unbox_be.domain.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Getter
@AllArgsConstructor // 테스트 코드에서 new 생성자를 쓰기 위해 반드시 필요
@NoArgsConstructor
public class ReviewRequestDto {
    private UUID orderId; // 서버가 이 ID를 통해 상품 정보를 찾으므로 productId는 불필요
    private String content;
    private Integer rating;
    private String imageUrl;
}