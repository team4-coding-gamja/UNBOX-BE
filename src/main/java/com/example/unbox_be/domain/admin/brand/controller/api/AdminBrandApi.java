package com.example.unbox_be.domain.admin.brand.controller.api;

import com.example.unbox_be.domain.admin.brand.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.domain.admin.brand.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "관리자 브랜드", description = "관리자 브랜드 관리 API")
@RequestMapping("/api/admin/brands")
public interface AdminBrandApi {

    @Operation(
            summary = "브랜드 등록",
            description = "관리자가 브랜드를 등록합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 등록 성공",
                    content = @Content(schema = @Schema(implementation = AdminBrandCreateResponseDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패 / 이미 존재하는 브랜드",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content
            )
    })
    @PostMapping
    ApiResponse<AdminBrandCreateResponseDto> createBrand(
            @RequestBody @Valid AdminBrandCreateRequestDto requestDto
    );

    @Operation(
            summary = "브랜드 삭제",
            description = "관리자가 브랜드를 삭제합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "브랜드 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "브랜드를 찾을 수 없음",
                    content = @Content
            )
    })
    @DeleteMapping("/{brandId}")
    ApiResponse<Void> deleteBrand(
            @Parameter(description = "브랜드 ID", example = "e7b7a8f9-1b2c-4d5e-8f90-123456789abc")
            @PathVariable UUID brandId
    );
}
