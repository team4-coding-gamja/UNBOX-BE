package com.example.unbox_be.domain.admin.order.controller.api;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "관리자 주문 관리", description = "관리자용 주문 조회 및 상태 변경 API")
@RequestMapping("/api/admin/orders")
public interface OrderAdminApi {

    @Operation(summary = "전체 주문 목록 조회", description = "조건에 맞는 전체 주문 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getAdminOrders(
            @ParameterObject @ModelAttribute OrderSearchCondition condition,
            @PageableDefault(size = 10) Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    ResponseEntity<ApiResponse<OrderDetailResponseDto>> getAdminOrderDetail(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 상태 변경", description = "주문 상태를 변경합니다 (예: 검수 합격, 배송 시작).")
    @PatchMapping("/{orderId}/status")
    ResponseEntity<ApiResponse<OrderDetailResponseDto>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
