package com.example.unbox_user.user.request.controller.api;

import com.example.unbox_user.user.request.dto.request.AdminProductRequestUpdateRequestDto;
import com.example.unbox_user.user.request.dto.response.AdminProductRequestListResponseDto;
import com.example.unbox_user.user.request.dto.response.AdminProductRequestUpdateResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 상품 등록 요청 관리", description = "관리자용 상품 등록 요청 관리 API")
@RequestMapping("/api/admin/product-requests")
public interface AdminProductRequestApi {

    // ✅ 상품 요청 목록 조회 (페이징)
    @Operation(
            summary = "상품 요청 목록 조회",
            description = "상품 등록 요청 목록을 최신순으로 페이징 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    CustomApiResponse<Page<AdminProductRequestListResponseDto>> getProductRequests(
            @ParameterObject
            @Parameter(description = "페이징 정보 (page, size, sort)", required = false)
            Pageable pageable
    );

    // ✅ 상품 요청 상태 변경 (승인/반려 등)
    @Operation(
            summary = "상품 요청 상태 변경",
            description = "상품 등록 요청의 상태를 변경합니다. (예: APPROVED / REJECTED)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "요청 데이터 없음")
    })
    @PatchMapping("/{productRequestId}/status")
    CustomApiResponse<AdminProductRequestUpdateResponseDto> updateProductRequestStatus(
            @Parameter(description = "상품 요청 ID", required = true)
            @PathVariable UUID productRequestId,

            @RequestBody @Valid
            AdminProductRequestUpdateRequestDto requestDto
    );
}
