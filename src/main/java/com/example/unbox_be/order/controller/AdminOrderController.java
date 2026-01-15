package com.example.unbox_be.order.controller;

import com.example.unbox_be.order.controller.api.AdminOrderApi;
import com.example.unbox_be.order.dto.OrderSearchCondition;
import com.example.unbox_be.order.service.AdminOrderService;
import com.example.unbox_be.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.order.dto.response.OrderResponseDto;
import com.example.unbox_be.common.pagination.PageSizeLimiter;
import com.example.unbox_be.common.response.CustomApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderController implements AdminOrderApi {

    private final AdminOrderService orderAdminService;

    // ✅ 주문 목록 조회
    @GetMapping
    public CustomApiResponse<Page<OrderResponseDto>> getAdminOrders(
            @ModelAttribute OrderSearchCondition condition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Pageable limited = PageSizeLimiter.limit(pageable);
        Page<OrderResponseDto> response = orderAdminService.getAdminOrders(condition, limited);
        return CustomApiResponse.success(response);
    }

    // ✅ 주문 상세 조회
    @Override
    @GetMapping("{orderId}")
    public CustomApiResponse<OrderDetailResponseDto> getAdminOrderDetail(
            @PathVariable UUID orderId) {
        OrderDetailResponseDto response = orderAdminService.getAdminOrderDetail(orderId);
        return CustomApiResponse.success(response);
    }

    // ✅ 주문 상태 변경
    @Override
    @PatchMapping("/{orderId}/status")
    public CustomApiResponse<OrderDetailResponseDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody @Valid OrderStatusUpdateRequestDto requestDto) {
        OrderDetailResponseDto response = orderAdminService.updateAdminStatus(orderId, requestDto);
        return CustomApiResponse.success(response);
    }
}
