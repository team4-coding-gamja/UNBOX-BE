package com.example.unbox_be.domain.product.controller.api;

import com.example.unbox_be.domain.product.dto.response.BrandListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductDetailResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductListResponseDto;
import com.example.unbox_be.domain.product.dto.response.ProductOptionListResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "상품", description = "상품 조회 API")
@RequestMapping("/api/products")
public interface ProductApi {

    @Operation(
            summary = "상품 목록 조회 (검색 + 페이징)",
            description = """
                    상품 목록을 조회합니다.
                    - brandId: 브랜드 필터(선택)
                    - category: 카테고리 필터(선택)
                    - keyword: 상품명/모델번호 검색(선택)
                    - pageable: page/size/sort
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
    })
    @GetMapping
    CustomApiResponse<Page<ProductListResponseDto>> getProducts(
            @Parameter(description = "브랜드 ID(선택)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam(required = false) UUID brandId,

            @Parameter(description = "카테고리(선택) - 예: SNEAKERS", example = "SNEAKERS")
            @RequestParam(required = false) String category,

            @Parameter(description = "검색 키워드(선택) - 상품명/모델번호", example = "dunk")
            @RequestParam(required = false) String keyword,

            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상품 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @GetMapping("/{productId}")
    CustomApiResponse<ProductDetailResponseDto> getProductDetail(
            @Parameter(description = "상품 ID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID productId
    );

    @Operation(
            summary = "상품 옵션(사이즈)별 최저가 조회",
            description = "특정 상품(productId)의 옵션(사이즈)별 최저가 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "옵션별 최저가 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @GetMapping("/{productId}/options")
    CustomApiResponse<List<ProductOptionListResponseDto>> getProductOptions(
            @Parameter(description = "상품 ID", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID productId
    );

    @Operation(
            summary = "브랜드 전체 조회",
            description = "등록된 모든 브랜드를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "브랜드 전체 조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            )
    })
    @GetMapping("/brands")
    CustomApiResponse<List<BrandListResponseDto>> getAllBrands();
}
