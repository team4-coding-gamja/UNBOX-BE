package com.example.unbox_be.product.reviews.service;

import com.example.unbox_be.product.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.product.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.product.reviews.dto.response.ReviewUpdateResponseDto;

import java.util.UUID;

public interface ReviewService  {

    // ✅ 리뷰 생성
    ReviewCreateResponseDto createReview(Long userId, ReviewCreateRequestDto requestDto);
    // ✅ 리뷰 상세 조회
    ReviewDetailResponseDto getReview(UUID reviewId);
    // ✅ 리뷰 수정
    ReviewUpdateResponseDto updateReview(Long userId, UUID reviewId, ReviewUpdateRequestDto requestDto);
    // ✅ 리뷰 삭제
    void deleteReview(Long userId, UUID reviewId, String deletedBy);
}