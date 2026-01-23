package com.example.unbox_product.reviews.service;

import com.example.unbox_product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_product.reviews.dto.response.ReviewListResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface ReviewService  {

    // ✅ 리뷰 생성
    ReviewCreateResponseDto createReview(Long userId, ReviewCreateRequestDto requestDto);
    // ✅ 리뷰 상세 조회
    ReviewDetailResponseDto getReview(UUID reviewId);
    // ✅ 리뷰 수정
    ReviewUpdateResponseDto updateReview(Long userId, UUID reviewId, ReviewUpdateRequestDto requestDto);
    // ✅ 리뷰 삭제
    void deleteReview(Long userId, UUID reviewId, String deletedBy);

    // ✅ 상품별 리뷰 목록 조회 (AI 요약용)
    List<ReviewListResponseDto> getReviewsByProduct(UUID productId);

    // ✅ 내가 쓴 리뷰 목록 조회
    Page<ReviewListResponseDto> getMyReviews(Long userId, Pageable pageable);
}
