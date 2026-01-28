package com.example.unbox_product.reviews.controller;

import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.reviews.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

@Tag(name = "[내부] 리뷰 관리", description = "내부 시스템용 리뷰 API")
@RestController
@RequestMapping("/internal/reviews")
@RequiredArgsConstructor
public class ReviewInternalController {

    private final ReviewService reviewService;

    // ✅ 상품별 리뷰 목록 조회 (AI 요약용)
    @Operation(summary = "상품별 리뷰 목록 조회", description = "특정 상품의 리뷰 목록을 조회합니다 (AI 요약 등).")
    @GetMapping("/products/{productId}")
    public List<ReviewListResponseDto> getReviewsByProduct(@PathVariable UUID productId) {
        return reviewService.getReviewsByProduct(productId);
    }
}
