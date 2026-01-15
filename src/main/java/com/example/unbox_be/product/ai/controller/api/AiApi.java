package com.example.unbox_be.product.ai.controller.api;

import com.example.unbox_be.product.ai.dto.response.AiReviewSummaryResponseDto;
import com.example.unbox_be.common.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "AI", description = "AI 기능 API (리뷰 요약 등)")
@RequestMapping("/api/ai")
public interface AiApi {

    @Operation(
            summary = "상품 리뷰 요약",
            description = "특정 상품(productId)의 리뷰들을 AI로 요약하여 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리뷰 요약 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (UUID 형식 오류 등)"),
            @ApiResponse(responseCode = "404", description = "상품 또는 리뷰 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류 또는 AI 연동 실패")
    })
    @GetMapping("/reviews/summary/{productId}")
    CustomApiResponse<AiReviewSummaryResponseDto> getReviewSummary(
            @Parameter(description = "리뷰 요약 대상 상품 ID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID productId
    );
}
