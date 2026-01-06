package com.example.unbox_be.domain.admin.product.controller.api;

import com.example.unbox_be.domain.admin.product.dto.request.AdminProductCreateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.request.AdminProductUpdateRequestDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductCreateResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductListResponseDto;
import com.example.unbox_be.domain.admin.product.dto.response.AdminProductUpdateResponseDto;
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

@Tag(name = "관리자 - 상품", description = "관리자 상품 / 옵션 관리 API")
@RequestMapping("/api/admin/products")
public interface AdminProductApi {

    @Operation(
            summary = "상품 목록 조회",
            description = "관리자가 상품 목록을 조회합니다. (검색, 필터링, 페이징 지원)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomApiResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    CustomApiResponse<Page<AdminProductListResponseDto>> getProducts(
            @Parameter(description = "브랜드 ID (필터링)")
            @RequestParam(required = false) UUID brandId,

            @Parameter(description = "카테고리 (필터링)")
            @RequestParam(required = false) String category,

            @Parameter(description = "검색어 (상품명, 모델번호)")
            @RequestParam(required = false) String keyword,

            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    );

    @Operation(
            summary = "상품 등록",
            description = "관리자가 상품을 등록합니다. brandId는 RequestBody로 전달합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 등록 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomApiResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "브랜드를 찾을 수 없음")
    })
    @PostMapping
    CustomApiResponse<AdminProductCreateResponseDto> createProduct(
            @RequestBody @Valid AdminProductCreateRequestDto requestDto
    );

    @Operation(
            summary = "상품 수정",
            description = "관리자가 상품 정보를 수정합니다. (MASTER, MANAGER 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상품 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "상품/브랜드를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "모델번호 중복")
    })
    @PatchMapping("/{productId}")
    CustomApiResponse<AdminProductUpdateResponseDto> updateProduct(

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId,

            @RequestBody @Valid AdminProductUpdateRequestDto requestDto
    );

    @Operation(
            summary = "상품 삭제",
            description = "관리자가 상품을 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 삭제 성공",
                    content = @Content(
                            schema = @Schema(implementation = CustomApiResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @DeleteMapping("/{productId}")
    CustomApiResponse<Void> deleteProduct(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "상품 ID (UUID)", required = true)
            @PathVariable UUID productId
    );
}
