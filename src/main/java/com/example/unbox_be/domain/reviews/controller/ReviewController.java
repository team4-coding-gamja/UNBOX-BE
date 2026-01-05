package com.example.unbox_be.domain.reviews.controller;

import com.example.unbox_be.domain.reviews.controller.api.ReviewApi;
import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewResponseDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.service.ReviewService;
import com.example.unbox_be.global.response.CustomApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController implements ReviewApi {

    private final ReviewService reviewService;

    @Override
    @PostMapping
    public ResponseEntity<CustomApiResponse<UUID>> create(ReviewRequestDto dto, Long userId) {
        return ResponseEntity.ok(CustomApiResponse.success(reviewService.createReview(dto, userId)));
    }

    @Override
    @GetMapping
    public ResponseEntity<CustomApiResponse<Page<ReviewResponseDto>>> getList(
            @RequestParam UUID productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(CustomApiResponse.success(reviewService.getReviewsByProduct(productId, pageable)));
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<CustomApiResponse<Void>> update(UUID reviewId, ReviewUpdateDto dto, Long userId) {
        reviewService.updateReview(reviewId, dto, userId);
        return ResponseEntity.ok(CustomApiResponse.successWithNoData());
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<CustomApiResponse<Void>> delete(UUID reviewId, Long userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(CustomApiResponse.successWithNoData());
    }
}