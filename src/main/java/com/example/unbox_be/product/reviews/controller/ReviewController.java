package com.example.unbox_be.product.reviews.controller;

import com.example.unbox_be.product.reviews.controller.api.ReviewApi;
import com.example.unbox_be.product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.product.reviews.service.ReviewService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    // ✅ 리뷰 생성
    @PostMapping
    public CustomApiResponse<ReviewCreateResponseDto> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ReviewCreateRequestDto requestDto) {
        ReviewCreateResponseDto result = reviewService.createReview(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 상세 조회
    @GetMapping("/{reviewId}")
    public CustomApiResponse<ReviewDetailResponseDto> getReview(
            @PathVariable UUID reviewId) {
        ReviewDetailResponseDto result = reviewService.getReview(reviewId);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 수정
    @PatchMapping("/{reviewId}")
    public CustomApiResponse<ReviewUpdateResponseDto> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewUpdateRequestDto requestDto) {
        ReviewUpdateResponseDto result = reviewService.updateReview(userDetails.getUserId(), reviewId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public CustomApiResponse<Void> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId) {
        String deleteBy = userDetails.getUsername();
        reviewService.deleteReview(userDetails.getUserId(), reviewId, deleteBy);
        return CustomApiResponse.success(null);
    }
}