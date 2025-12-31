package com.example.unbox_be.domain.order.controller;

import com.example.unbox_be.domain.order.dto.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.OrderResponseDto;
import com.example.unbox_be.domain.order.service.OrderService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        // (로그인이 안 되어있으면 userDetails가 null일 수 있으나, SecurityConfig 설정상 /api/**는 인증 필요하므로 안전)
        String buyerEmail = userDetails.getUsername();

        log.info("[OrderController] 주문 요청자: {}", buyerEmail);

        // 2. 서비스 호출 (이메일 넘김)
        OrderResponseDto responseDto = orderService.createOrder(requestDto, buyerEmail);

        // 3. 공통 응답 포맷(ApiResponse)으로 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseDto));
    }
}