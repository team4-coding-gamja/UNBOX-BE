package com.example.unbox_product.reviews.controller;

import com.example.unbox_product.reviews.controller.api.ReviewApi;
import com.example.unbox_product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_product.reviews.service.ReviewService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    // ✅ 리뷰 생성
    @Override
    @PostMapping
    public CustomApiResponse<ReviewCreateResponseDto> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ReviewCreateRequestDto requestDto) {
        ReviewCreateResponseDto result = reviewService.createReview(userDetails.getUserId(), requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 내 리뷰 목록 조회
    @Override
    @GetMapping("/my-reviews")
    public CustomApiResponse<Page<ReviewListResponseDto>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        var result = reviewService.getMyReviews(userDetails.getUserId(), pageable);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 상세 조회
    @Override
    @GetMapping("/{reviewId}")
    public CustomApiResponse<ReviewDetailResponseDto> getReview(
            @PathVariable UUID reviewId) {
        ReviewDetailResponseDto result = reviewService.getReview(reviewId);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 수정
    @Override
    @PatchMapping("/{reviewId}")
    public CustomApiResponse<ReviewUpdateResponseDto> updateReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewUpdateRequestDto requestDto) {
        ReviewUpdateResponseDto result = reviewService.updateReview(userDetails.getUserId(), reviewId, requestDto);
        return CustomApiResponse.success(result);
    }

    // ✅ 리뷰 삭제
    @Override
    @DeleteMapping("/{reviewId}")
    public CustomApiResponse<Void> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID reviewId) {
        String deleteBy = userDetails.getUsername();
        reviewService.deleteReview(userDetails.getUserId(), reviewId, deleteBy);
        return CustomApiResponse.success(null);
    }
}
