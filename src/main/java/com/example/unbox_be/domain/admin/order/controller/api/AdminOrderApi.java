package com.example.unbox_be.domain.admin.order.controller.api;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse; // ApiResponse -> CustomApiResponse로 변경
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "관리자 주문 관리", description = "관리자용 주문 조회 및 상태 변경 API")
public interface AdminOrderApi {

    @Operation(summary = "전체 주문 목록 조회", description = "검색 조건(상태, 키워드 등)과 페이징을 이용해 주문 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    CustomApiResponse<Page<OrderResponseDto>> getAdminOrders(
            @ParameterObject @ModelAttribute OrderSearchCondition condition, // 스웨거에서 필드별로 노출
            @ParameterObject @PageableDefault(size = 10) Pageable pageable,   // 페이징 파라미터 최적화
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 상세 조회", description = "특정 주문의 상세 정보 및 상품 내역을 조회합니다.")
    @GetMapping("/{orderId}")
    CustomApiResponse<OrderDetailResponseDto> getAdminOrderDetail(
            @Parameter(description = "주문 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 상태 변경", description = "관리자가 주문의 상태를 변경하고 운송장 번호를 입력합니다.")
    @PatchMapping("/{orderId}/status")
    CustomApiResponse<OrderDetailResponseDto> updateOrderStatus(
            @Parameter(description = "주문 UUID") @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}