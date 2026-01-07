package com.example.unbox_be.domain.reviews.controller.api;

import com.example.unbox_be.domain.reviews.dto.request.ReviewCreateRequestDto;
import com.example.unbox_be.domain.reviews.dto.request.ReviewUpdateRequestDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewCreateResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewDetailResponseDto;
import com.example.unbox_be.domain.reviews.dto.response.ReviewUpdateResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "리뷰 관리", description = "리뷰 생성 / 조회 / 수정 / 삭제 API")
@RequestMapping("/api/reviews")
public interface ReviewApi {

    // ✅ 리뷰 생성
    @Operation(
            summary = "리뷰 생성",
            description = "완료된 주문(Order)에 대해 리뷰를 작성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 주문 상태가 리뷰 작성 불가"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (구매자 아님)"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PostMapping
    CustomApiResponse<ReviewCreateResponseDto> createReview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid ReviewCreateRequestDto requestDto
    );

    // ✅ 리뷰 상세 조회
    @Operation(
            summary = "리뷰 상세 조회",
            description = "리뷰 ID로 리뷰 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 조회 성공"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @GetMapping("/{reviewId}")
    CustomApiResponse<ReviewDetailResponseDto> getReview(
            @PathVariable UUID reviewId
    );

    // ✅ 리뷰 수정
    @Operation(
            summary = "리뷰 수정",
            description = "본인이 작성한 리뷰의 내용/평점/이미지를 수정합니다. (PATCH)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (리뷰 작성자 아님)"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @PatchMapping("/{reviewId}")
    CustomApiResponse<ReviewUpdateResponseDto> updateReview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @PathVariable UUID reviewId,
            @RequestBody @Valid ReviewUpdateRequestDto requestDto
    );

    // ✅ 리뷰 삭제
    @Operation(
            summary = "리뷰 삭제",
            description = "본인이 작성한 리뷰를 삭제합니다. (Soft Delete)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (리뷰 작성자 아님)"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @DeleteMapping("/{reviewId}")
    CustomApiResponse<Void> deleteReview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @PathVariable UUID reviewId
    );
}
