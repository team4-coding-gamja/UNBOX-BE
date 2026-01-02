package com.example.unbox_be.domain.order.controller;

import com.example.unbox_be.domain.order.controller.api.OrderApi;
import com.example.unbox_be.domain.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.request.OrderTrackingRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.service.OrderService;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<ApiResponse<UUID>> createOrder(
            OrderCreateRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        // 컨트롤러는 오직 '요청 수신'과 '응답 반환'에만 집중합니다.
        UUID orderId = orderService.createOrder(requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderId));
    }

    @Override
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getMyOrders(
            CustomUserDetails userDetails,
            Pageable pageable
    ) {
        // 페이지 사이즈 검증 로직은 Service 내부로 이동시켰습니다.
        Page<OrderResponseDto> response = orderService.getMyOrders(userDetails.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getOrderDetail(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        // 여기서는 ID가 더 명확하므로 ID를 사용하되, 서비스 로직에 따라 유동적입니다.
        OrderDetailResponseDto response = orderService.getOrderDetail(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> cancelOrder(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.cancelOrder(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> registerTracking(
            UUID orderId,
            OrderTrackingRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.registerTracking(
                orderId,
                requestDto.getTrackingNumber(),
                userDetails.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> updateOrderStatus(
            UUID orderId,
            OrderStatusUpdateRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.updateAdminStatus(
                orderId,
                requestDto.getStatus(),
                requestDto.getTrackingNumber(),
                userDetails.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> confirmOrder(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        OrderDetailResponseDto response = orderService.confirmOrder(orderId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}