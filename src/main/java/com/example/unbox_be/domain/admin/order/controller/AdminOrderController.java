package com.example.unbox_be.domain.admin.order.controller;

import com.example.unbox_be.domain.admin.order.controller.api.AdminOrderApi;
import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.admin.order.service.AdminOrderService;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.global.pagination.PageSizeLimiter;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController implements AdminOrderApi {

    private final AdminOrderService orderAdminService;

    @Override
    @GetMapping
    public CustomApiResponse<Page<OrderResponseDto>> getAdminOrders(
            @ModelAttribute OrderSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<OrderResponseDto> response = orderAdminService.getAdminOrders(condition, limited);
        return CustomApiResponse.success(response);
    }

    @Override
    @GetMapping("{orderId}")
    public CustomApiResponse<OrderDetailResponseDto> getAdminOrderDetail(
            @PathVariable UUID orderId) {
        OrderDetailResponseDto response = orderAdminService.getAdminOrderDetail(orderId);
        return CustomApiResponse.success(response);
    }

    @Override
    @PatchMapping("/{orderId}/status")
    public CustomApiResponse<OrderDetailResponseDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody @Valid OrderStatusUpdateRequestDto requestDto) {
        OrderDetailResponseDto response = orderAdminService.updateAdminStatus(orderId, requestDto);
        return CustomApiResponse.success(response);
    }
}
