package com.example.unbox_be.domain.reviews.controller;

import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.domain.reviews.entity.Review;
import com.example.unbox_be.domain.reviews.service.ReviewService;
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
public class ReviewController {

    private final ReviewService reviewService;

    // POST
    @PostMapping
    public ResponseEntity<UUID> create(@RequestBody ReviewRequestDto dto /*, @RequestHeader("X-User-Id") Long userId */) {
        Long tempUserId = 1L; // 테스트용 임시 ID
        return ResponseEntity.ok(reviewService.createReview(dto, tempUserId));
    }

    // GET
    @GetMapping
    public ResponseEntity<Page<Review>> getList(@RequestParam UUID productId,
                                                @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    // PATCH
    @PatchMapping("/{reviewId}")
    public ResponseEntity<Void> update(@PathVariable UUID reviewId, @RequestBody ReviewUpdateDto dto, @RequestHeader("X-User-Id") Long userId) {
        reviewService.updateReview(reviewId, dto, userId);
        return ResponseEntity.ok().build();
    }

    // Delete
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reviewId, @RequestHeader("X-User-Id") String userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}