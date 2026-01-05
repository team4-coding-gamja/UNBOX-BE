package com.example.unbox_be.domain.reviews.controller.api;

import com.example.unbox_be.domain.reviews.dto.ReviewRequestDto;
import com.example.unbox_be.domain.reviews.dto.ReviewResponseDto;
import com.example.unbox_be.domain.reviews.dto.ReviewUpdateDto;
import com.example.unbox_be.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "리뷰 관리", description = "리뷰 생성 / 조회 / 수정 / 삭제 API")
public interface ReviewApi {

    @Operation(summary = "리뷰 생성", description = "완료된 주문(Order)에 대해 리뷰를 작성합니다.")
    ResponseEntity<ApiResponse<UUID>> create(
            @RequestBody ReviewRequestDto dto,
            @RequestHeader("X-User-Id") Long userId
    );

    @Operation(summary = "상품별 리뷰 목록 조회", description = "특정 상품 ID를 기준으로 삭제되지 않은 리뷰들을 조회합니다.")
    ResponseEntity<ApiResponse<Page<ReviewResponseDto>>> getList(
            @RequestParam UUID productId,
            @Parameter(description = "페이징 파라미터") Pageable pageable
    );

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰의 내용과 평점을 수정합니다.")
    ResponseEntity<ApiResponse<Void>> update(
            @PathVariable UUID reviewId,
            @RequestBody ReviewUpdateDto dto,
            @RequestHeader("X-User-Id") Long userId
    );

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제(Soft Delete)합니다.")
    ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID reviewId,
            @RequestHeader("X-User-Id") Long userId
    );
}