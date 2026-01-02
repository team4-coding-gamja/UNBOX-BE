package com.example.unbox_be.domain.admin.product.controller.api;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductOptionCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductOptionCreateResponseDto;
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

@Tag(name = "관리자 상품", description = "관리자 상품 / 옵션 관리 API")
@RequestMapping("/api/admin/products")
public interface AdminProductApi {

    @Operation(
            summary = "상품 등록",
            description = "관리자가 상품을 등록합니다. brandId는 RequestBody로 전달합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상품 등록 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "브랜드를 찾을 수 없음")
    })
    @PostMapping
    ApiResponse<AdminProductCreateResponseDto> createProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid AdminProductCreateRequestDto requestDto
    );

    @Operation(
            summary = "상품 삭제",
            description = "관리자가 상품을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상품 삭제 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @DeleteMapping("/{productId}")
    ApiResponse<Void> deleteProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId
    );

    @Operation(
            summary = "상품 옵션 등록",
            description = "특정 상품에 옵션을 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "옵션 등록 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 옵션")
    })
    @PostMapping("/{productId}/options")
    ApiResponse<AdminProductOptionCreateResponseDto> createProductOption(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId,

            @RequestBody @Valid AdminProductOptionCreateRequestDto requestDto
    );

    @Operation(
            summary = "상품 옵션 삭제",
            description = "특정 상품의 옵션을 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "옵션 삭제 성공",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "옵션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "해당 상품에 속하지 않은 옵션")
    })
    @DeleteMapping("/{productId}/options/{optionId}")
    ApiResponse<Void> deleteProductOption(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId,

            @Parameter(description = "옵션 ID (UUID)", required = true)
            @PathVariable UUID optionId
    );
}
