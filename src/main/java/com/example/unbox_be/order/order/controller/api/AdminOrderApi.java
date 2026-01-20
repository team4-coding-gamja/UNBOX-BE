package com.example.unbox_be.order.order.controller.api;

import com.example.unbox_be.order.order.dto.OrderSearchCondition;
import com.example.unbox_be.order.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.order.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.order.order.dto.response.OrderResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "[관리자] 주문 관리", description = "관리자 주문 관리 API")
@RequestMapping("/api/admin/orders")
public interface AdminOrderApi {

    @Operation(
            summary = "관리자 주문 목록 조회",
            description = """
                    관리자 주문 목록을 검색 조건(상태/키워드/기간 등)으로 조회합니다.
                    Pageable을 통해 페이징/정렬이 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    CustomApiResponse<Page<OrderResponseDto>> getAdminOrders(
            @Parameter(description = "검색 조건(쿼리스트링)")
            @ModelAttribute OrderSearchCondition condition,

            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "관리자 주문 상세 조회",
            description = "주문 UUID로 주문 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @GetMapping("/{orderId}")
    CustomApiResponse<OrderDetailResponseDto> getAdminOrderDetail(
            @Parameter(description = "주문 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId
    );

    @Operation(
            summary = "관리자 주문 상태 변경",
            description = """
                    주문 상태를 변경합니다.
                    - 상태가 SHIPPED_TO_BUYER 인 경우 운송장 번호(trackingNumber)가 필수입니다.
                    - 상태 전이 규칙은 Order 도메인 로직에서 검증됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = OrderDetailResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "요청 값/상태 전이 규칙 위반"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
    })
    @PatchMapping("/{orderId}/status")
    CustomApiResponse<OrderDetailResponseDto> updateOrderStatus(
            @Parameter(description = "주문 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,

            @Valid @RequestBody OrderStatusUpdateRequestDto requestDto
    );
}