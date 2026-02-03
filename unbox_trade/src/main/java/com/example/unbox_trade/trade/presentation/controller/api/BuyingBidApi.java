package com.example.unbox_trade.trade.presentation.controller.api;

import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.BuyingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidCreateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidDetailResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidListResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.BuyingBidsPriceUpdateResponseDto;
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

@Tag(name = "[사용자] 구매입찰 관리", description = "구매입찰 관리 API")
public interface BuyingBidApi {

    @Operation(summary = "구매 입찰 생성", description = "로그인한 사용자가 구매 입찰을 생성합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = BuyingBidCreateRequestDto.class), examples = {
            @io.swagger.v3.oas.annotations.media.ExampleObject(name = "구매 입찰(50000원)", value = """
                    {
                      "productOptionId": "aaaa0000-0000-0000-0000-000000000001",
                      "price": 50000
                    }
                    """),
            @io.swagger.v3.oas.annotations.media.ExampleObject(name = "구매 입찰(45000원)", value = """
                    {
                      "productOptionId": "aaaa0000-0000-0000-0000-000000000001",
                      "price": 45000
                    }
                    """)
    }))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "구매 입찰 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @PostMapping
    CustomApiResponse<BuyingBidCreateResponseDto> createBuyingBid(
            @RequestBody @Valid BuyingBidCreateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "구매 입찰 취소", description = "로그인한 사용자가 본인의 구매 입찰을 취소합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "구매 입찰 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "구매 입찰을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{buyingId}")
    CustomApiResponse<Void> cancelBuyingBid(
            @Parameter(description = "구매 입찰 ID", required = true) @PathVariable UUID buyingId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "구매 입찰 가격 변경", description = "로그인한 사용자가 본인의 구매 입찰 가격을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구매 입찰 가격 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 가격 변경 불가 상태", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "구매 입찰을 찾을 수 없음", content = @Content)
    })
    @PatchMapping("/{buyingId}/price")
    CustomApiResponse<BuyingBidsPriceUpdateResponseDto> updatePrice(
            @Parameter(description = "구매 입찰 ID", required = true) @PathVariable UUID buyingId,
            @RequestBody @Valid BuyingBidsPriceUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "구매 입찰 상세 조회", description = "구매 입찰 ID로 상세 정보를 조회합니다. (본인 입찰만 조회 가능)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구매 입찰 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음(본인 입찰 아님)", content = @Content),
            @ApiResponse(responseCode = "404", description = "구매 입찰을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{buyingId}")
    CustomApiResponse<BuyingBidDetailResponseDto> getBuyingBidDetail(
            @Parameter(description = "구매 입찰 ID", required = true) @PathVariable UUID buyingId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "내 구매 입찰 목록 조회", description = "로그인한 사용자의 구매 입찰 목록을 최신순으로 Slice 페이징 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 구매 입찰 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping("/my")
    CustomApiResponse<Slice<BuyingBidListResponseDto>> getMyBuyingBids(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);
}
