package com.example.unbox_be.domain.admin.order.controller;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.admin.order.controller.api.OrderAdminApi;
import com.example.unbox_be.domain.admin.order.service.OrderAdminService;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.global.response.ApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderAdminController implements OrderAdminApi {

    private final OrderAdminService orderAdminService;

    @Override
    public ResponseEntity<ApiResponse<Page<OrderResponseDto>>> getAdminOrders(
            OrderSearchCondition condition,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        Page<OrderResponseDto> response = orderAdminService.getAdminOrders(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> getAdminOrderDetail(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        Long adminId = userDetails.getAdminId();
        OrderDetailResponseDto response = orderAdminService.getAdminOrderDetail(orderId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Override
    public ResponseEntity<ApiResponse<OrderDetailResponseDto>> updateOrderStatus(
            UUID orderId,
            OrderStatusUpdateRequestDto requestDto,
            CustomUserDetails userDetails
    ) {
        Long adminId = userDetails.getAdminId();
        OrderDetailResponseDto response = orderAdminService.updateAdminStatus(
                orderId,
                requestDto.getStatus(),
                requestDto.getTrackingNumber(),
                adminId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
