package com.example.unbox_be.domain.order.controller.api;

import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.request.OrderTrackingRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "주문 관리", description = "주문 생성, 조회, 취소, 상태 변경 API")
@RequestMapping("/api/orders")
public interface OrderApi {

    @Operation(summary = "주문 생성", description = "구매자가 상품을 주문합니다.")
    @PostMapping
    ResponseEntity<CustomApiResponse<UUID>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "내 주문 목록 조회", description = "구매자가 자신의 주문 내역을 페이징 조회합니다.")
    @GetMapping
    ResponseEntity<CustomApiResponse<Page<OrderResponseDto>>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable
    );

    @Operation(summary = "주문 상세 조회", description = "주문의 상세 정보(배송지, 옵션 등)를 조회합니다.")
    @GetMapping("/{orderId}")
    ResponseEntity<CustomApiResponse<OrderDetailResponseDto>> getOrderDetail(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "주문 취소", description = "주문을 취소합니다.")
    @PatchMapping("/{orderId}/cancel")
    ResponseEntity<CustomApiResponse<OrderDetailResponseDto>> cancelOrder(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "운송장 등록 (판매자용)", description = "판매자가 운송장 번호를 등록하고 배송을 시작합니다.")
    @PatchMapping("/{orderId}/tracking")
    ResponseEntity<CustomApiResponse<OrderDetailResponseDto>> registerTracking(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderTrackingRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "구매 확정 (구매자용)", description = "배송 완료된 주문을 구매 확정합니다.")
    @PatchMapping("/{orderId}/confirm")
    ResponseEntity<CustomApiResponse<OrderDetailResponseDto>> confirmOrder(
            @PathVariable UUID orderId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );
}