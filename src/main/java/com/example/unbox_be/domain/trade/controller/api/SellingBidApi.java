package com.example.unbox_be.domain.trade.controller.api;

import com.example.unbox_be.domain.trade.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.domain.trade.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidsPriceUpdateResponseDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "판매입찰 관리", description = "판매입찰 관리 API")
public interface SellingBidApi {

        // ✅ 1) 판매 입찰 생성
        @Operation(summary = "판매 입찰 생성", description = "로그인한 사용자가 판매 입찰을 생성합니다.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = SellingBidCreateRequestDto.class), examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "판매 입찰(10000원)", value = """
                                        {
                                          "userId": 5,
                                          "optionId": "aaaa0000-0000-0000-0000-000000000001",
                                          "price": 10000
                                        }
                                        """),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(name = "판매 입찰(5000원)", value = """
                                        {
                                          "userId": 5,
                                          "optionId": "aaaa0000-0000-0000-0000-000000000001",
                                          "price": 5000
                                        }
                                        """)

        }))
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "판매 입찰 생성 성공"),
                        @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
        })
        @PostMapping
        CustomApiResponse<SellingBidCreateResponseDto> createSellingBid(
                        @RequestBody @Valid SellingBidCreateRequestDto requestDto,

                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

        // ✅ 2) 판매 입찰 취소
        @Operation(summary = "판매 입찰 취소", description = "로그인한 사용자가 본인의 판매 입찰을 취소합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "판매 입찰 취소 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                        @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
                        @ApiResponse(responseCode = "404", description = "판매 입찰을 찾을 수 없음", content = @Content)
        })
        @DeleteMapping("/{sellingId}")
        CustomApiResponse<Void> cancelSellingBid(
                        @Parameter(description = "판매 입찰 ID", required = true) @PathVariable UUID sellingId,

                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

        // ✅ 3) 판매 입찰 가격 변경
        @Operation(summary = "판매 입찰 가격 변경", description = "로그인한 사용자가 본인의 판매 입찰 가격을 변경합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "판매 입찰 가격 변경 성공"),
                        @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 가격 변경 불가 상태", content = @Content),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                        @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
                        @ApiResponse(responseCode = "404", description = "판매 입찰을 찾을 수 없음", content = @Content)
        })
        @PatchMapping("/{sellingId}/price")
        CustomApiResponse<SellingBidsPriceUpdateResponseDto> updatePrice(
                        @Parameter(description = "판매 입찰 ID", required = true) @PathVariable UUID sellingId,

                        @RequestBody @Valid SellingBidsPriceUpdateRequestDto requestDto,

                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

        // ✅ 4) 판매 입찰 상세 조회
        @Operation(summary = "판매 입찰 상세 조회", description = "판매 입찰 ID로 상세 정보를 조회합니다. (본인 입찰만 조회 가능)")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "판매 입찰 상세 조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                        @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
                        @ApiResponse(responseCode = "404", description = "판매 입찰을 찾을 수 없음", content = @Content)
        })
        @GetMapping("/{sellingId}")
        CustomApiResponse<SellingBidDetailResponseDto> getSellingBidDetail(
                        @Parameter(description = "판매 입찰 ID", required = true) @PathVariable UUID sellingId,

                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

        // ✅ 5) 내 판매 입찰 목록 조회 (Slice 페이징)
        @Operation(summary = "내 판매 입찰 목록 조회", description = "로그인한 사용자의 판매 입찰 목록을 최신순으로 Slice 페이징 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "내 판매 입찰 목록 조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
        })
        @GetMapping("/my")
        CustomApiResponse<Slice<SellingBidListResponseDto>> getMySellingBids(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,

                        @ParameterObject @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);
}