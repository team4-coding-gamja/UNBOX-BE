package com.example.unbox_be.product.product.presentation.controller.api;

import com.example.unbox_be.product.product.presentation.dto.request.AdminBrandCreateRequestDto;
import com.example.unbox_be.product.product.presentation.dto.request.AdminBrandUpdateRequestDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandCreateResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandDetailResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandListResponseDto;
import com.example.unbox_be.product.product.presentation.dto.response.AdminBrandUpdateResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 브랜드 관리", description = "관리자용 브랜드 관리 API")
@RequestMapping("/api/admin/brands")
public interface AdminBrandApi {

    // ✅ 브랜드 목록 조회
    @Operation(
            summary = "브랜드 목록 조회",
            description = """
                    관리자가 등록된 브랜드 목록을 조회합니다. (MASTER, MANAGER 가능)
                    - keyword: 브랜드명 부분일치 검색
                    - pageable: size는 10/30/50만 허용되며 그 외 값은 10으로 고정됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "브랜드 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @GetMapping
    CustomApiResponse<Page<AdminBrandListResponseDto>> getBrands(
            @Parameter(description = "브랜드명 검색 키워드(부분일치)", example = "nike")
            @RequestParam(required = false) String keyword,

            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    );


    // ✅ 브랜드 상세 조회 (컨트롤러에 있는데 기존 스웨거에 없어서 추가)
    @Operation(
            summary = "브랜드 상세 조회",
            description = "브랜드 단건 상세 정보를 조회합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브랜드 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = AdminBrandDetailResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "브랜드를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{brandId}")
    CustomApiResponse<AdminBrandDetailResponseDto> getBrandDetail(
            @Parameter(description = "브랜드 ID", example = "e7b7a8f9-1b2c-4d5e-8f90-123456789abc")
            @PathVariable UUID brandId
    );


    // ✅ 브랜드 등록
    @Operation(
            summary = "브랜드 등록",
            description = "관리자가 브랜드를 등록합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브랜드 등록 성공",
                    content = @Content(schema = @Schema(implementation = AdminBrandCreateResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 이미 존재하는 브랜드", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PostMapping
    CustomApiResponse<AdminBrandCreateResponseDto> createBrand(
            @RequestBody @Valid AdminBrandCreateRequestDto requestDto
    );


    // ✅ 브랜드 수정
    @Operation(
            summary = "브랜드 수정",
            description = "관리자가 브랜드 정보를 수정합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브랜드 수정 성공",
                    content = @Content(schema = @Schema(implementation = AdminBrandUpdateResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 이미 존재하는 브랜드", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "브랜드를 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/{brandId}")
    CustomApiResponse<AdminBrandUpdateResponseDto> updateBrand(
            @Parameter(description = "브랜드 ID", example = "e7b7a8f9-1b2c-4d5e-8f90-123456789abc")
            @PathVariable UUID brandId,

            @RequestBody @Valid AdminBrandUpdateRequestDto requestDto
    );


    // ✅ 브랜드 삭제
    @Operation(
            summary = "브랜드 삭제",
            description = "관리자가 브랜드를 삭제합니다. (MASTER, MANAGER 가능) - Soft Delete"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "브랜드 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "브랜드를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{brandId}")
    CustomApiResponse<Void> deleteBrand(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "브랜드 ID", example = "e7b7a8f9-1b2c-4d5e-8f90-123456789abc")
            @PathVariable UUID brandId
    );
}
