package com.example.unbox_be.trade.presentation.controller.api;

import com.example.unbox_be.trade.presentation.dto.request.SellingBidSearchCondition;
import com.example.unbox_be.trade.presentation.dto.response.AdminSellingBidListResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = "[관리자] 판매입찰 관리", description = "관리자용 판매입찰 관리 API")
@RequestMapping("/api/admin/bids/selling")
public interface AdminSellingBidApi {

    @Operation(
            summary = "판매 입찰 목록 조회 (검색 기능 포함)",
            description = "다양한 조건(상태, 상품명, 브랜드명 등)으로 판매 입찰 목록을 검색하고 페이징 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @Parameters({
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(name = "size", description = "페이지 크기", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(name = "sort", description = "정렬 (필드명,ASC|DESC)", in = ParameterIn.QUERY, schema = @Schema(type = "string", example = "createdAt,DESC"))
    })
    @GetMapping
    CustomApiResponse<Page<AdminSellingBidListResponseDto>> getSellingBids(
            @Parameter(description = "검색 조건") @ModelAttribute SellingBidSearchCondition condition,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(
            summary = "판매 입찰 삭제",
            description = "특정 판매 입찰을 삭제(Soft Delete)합니다. 검수자(INSPECTOR)는 삭제할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "입찰 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @DeleteMapping("/{sellingId}")
    CustomApiResponse<Void> deleteSellingBid(
            @Parameter(description = "판매 입찰 UUID", required = true)
            @PathVariable UUID sellingId,

            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
