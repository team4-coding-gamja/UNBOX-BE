package com.example.unbox_be.domain.admin.order.controller;

import com.example.unbox_be.domain.admin.order.controller.api.AdminOrderApi;
import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.admin.order.service.AdminOrderService;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController implements AdminOrderApi {

    private final AdminOrderService orderAdminService;

    @Override
    @GetMapping
    public CustomApiResponse<Page<OrderResponseDto>> getAdminOrders(
            OrderSearchCondition condition,
            Pageable pageable,
            CustomUserDetails userDetails
    ) {
        Page<OrderResponseDto> response = orderAdminService.getAdminOrders(condition, pageable);
        return CustomApiResponse.success(response);
    }

    @Override
    @GetMapping("{orderId}")
    public CustomApiResponse<OrderDetailResponseDto> getAdminOrderDetail(
            UUID orderId,
            CustomUserDetails userDetails
    ) {
        Long adminId = userDetails.getAdminId();
        OrderDetailResponseDto response = orderAdminService.getAdminOrderDetail(orderId, adminId);
        return CustomApiResponse.success(response);
    }

    @Override
    @PutMapping("/{orderId}/status")
    public CustomApiResponse<OrderDetailResponseDto> updateOrderStatus(
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
        return CustomApiResponse.success(response);
    }
}
