package com.example.unbox_be.domain.admin.productOption.controller.api;

import com.example.unbox_be.domain.admin.productOption.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.productOption.dto.response.AdminProductOptionCreateResponseDto;
import com.example.unbox_be.domain.admin.productOption.dto.response.AdminProductOptionListResponseDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 상품 옵션 관리", description = "관리자용 상품 옵션 관리 API")
@RequestMapping("/api/admin/products/{productId}/options")
public interface AdminProductOptionApi {

    @Operation(
            summary = "상품 옵션 목록 조회",
            description = "전체 상품 옵션 목록을 조회하거나, 특정 상품 ID를 통해 필터링하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    CustomApiResponse<Page<AdminProductOptionListResponseDto>> getProductOptions(
            @Parameter(description = "상품 ID (UUID) - 특정 상품의 옵션만 조회할 때 사용")
            @PathVariable UUID productId,

            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "상품 옵션 등록",
            description = "특정 상품에 옵션을 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "옵션 등록 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomApiResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 옵션")
    })
    @PostMapping
    CustomApiResponse<AdminProductOptionCreateResponseDto> createProductOption(
            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId,

            @RequestBody @Valid AdminProductOptionCreateRequestDto requestDto
    );

    @Operation(
            summary = "상품 옵션 삭제",
            description = "특정 상품의 옵션을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "옵션 삭제 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomApiResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "옵션을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "해당 상품에 속하지 않은 옵션")
    })
    @DeleteMapping("{optionId}")
    CustomApiResponse<Void> deleteProductOption(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId,

            @Parameter(description = "옵션 ID (UUID)", required = true)
            @PathVariable UUID optionId
    );
}
