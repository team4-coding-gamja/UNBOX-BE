package com.example.unbox_product.reviews.controller;

import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.reviews.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
public class ReviewInternalController {

    private final ReviewService reviewService;

    // ✅ 상품별 리뷰 목록 조회 (AI 요약용)
    @GetMapping("/products/{productId}")
    public List<ReviewListResponseDto> getReviewsByProduct(@PathVariable UUID productId) {
        return reviewService.getReviewsByProduct(productId);
    }
}
