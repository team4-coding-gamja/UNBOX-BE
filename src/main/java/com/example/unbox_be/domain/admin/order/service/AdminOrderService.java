package com.example.unbox_be.domain.admin.order.service;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminOrderService {

    Page<OrderResponseDto> getAdminOrders(OrderSearchCondition condition, Pageable pageable);

    OrderDetailResponseDto getAdminOrderDetail(UUID orderId);

    OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatusUpdateRequestDto requestDto);
}
