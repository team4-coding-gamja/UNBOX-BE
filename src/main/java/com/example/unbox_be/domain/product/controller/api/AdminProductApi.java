package com.example.unbox_be.domain.product.controller.api;

import com.example.unbox_be.domain.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.AdminProductUpdateResponseDto;
import com.example.unbox_be.domain.product.dto.request.ProductSearchCondition;
import com.example.unbox_be.global.response.CustomApiResponse;

import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 상품 관리", description = "관리자 상품 관리 API")
@RequestMapping("/api/admin/products")
public interface AdminProductApi {

    @Operation(
            summary = "상품 목록 조회 (검색 + 페이징)",
            description = """
                    브랜드/카테고리/키워드 조건으로 상품 목록을 조회합니다.
                    - ProductSearchCondition: brandId, category, keyword
                    - Pageable: page, size, sort (기본 sort=createdAt,DESC)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminProductListResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음 (MASTER/MANAGER만 가능)", content = @Content)
    })
    @GetMapping
    CustomApiResponse<Page<AdminProductListResponseDto>> getProducts(
            @ParameterObject ProductSearchCondition condition,
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "상품 등록", description = "관리자가 상품을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = AdminProductCreateResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PostMapping
    CustomApiResponse<AdminProductCreateResponseDto> createProduct(
            //@Parameter(hidden = true)
            @RequestBody @Valid AdminProductCreateRequestDto requestDto
    );

    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = AdminProductUpdateResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "상품 없음", content = @Content)
    })
    @PatchMapping("/{productId}")
    CustomApiResponse<AdminProductUpdateResponseDto> updateProduct(
            @PathVariable UUID productId,
            @RequestBody @Valid AdminProductUpdateRequestDto requestDto
    );

    @Operation(summary = "상품 삭제", description = "상품을 삭제(소프트 삭제)합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "상품 없음", content = @Content)
    })
    @DeleteMapping("/{productId}")
    CustomApiResponse<Void> deleteProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId
    );
}
