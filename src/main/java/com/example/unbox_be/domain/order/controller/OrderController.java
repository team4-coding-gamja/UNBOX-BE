package com.example.unbox_be.domain.order.controller;

import com.example.unbox_be.domain.order.dto.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.service.OrderService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. 로그인한 유저의 이메일 추출
        String buyerEmail = userDetails.getUsername();

        // 2. 서비스 호출 (이메일 넘김)
        OrderResponseDto responseDto = orderService.createOrder(requestDto, buyerEmail);

        // 3. 공통 응답 포맷(ApiResponse)으로 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseDto));
    }

    // 내 구매 내역 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 페이지 사이즈 제한 정책 (10, 30, 50 이외에는 10으로 고정)
        int requestedSize = pageable.getPageSize();
        if (requestedSize != 10 && requestedSize != 30 && requestedSize != 50) {
            pageable = PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());
        }

        String email = userDetails.getUsername();
        Page<OrderResponseDto> responseDtoPage = orderService.getMyOrders(email, pageable);

        return ResponseEntity.ok(ApiResponse.success(responseDtoPage));
    }

    /**
     * 주문 상세 조회
     * - PathVariable로 orderId를 받음
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrderDetail(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getUsername();

        // 상세 조회 서비스 호출
        OrderDetailResponseDto detailDto = orderService.getOrderDetail(orderId, email);

        return ResponseEntity.ok(ApiResponse.success(detailDto));
    }
}