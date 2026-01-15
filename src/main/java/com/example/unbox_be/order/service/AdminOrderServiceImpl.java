package com.example.unbox_be.order.service;

import com.example.unbox_be.order.dto.OrderSearchCondition;
import com.example.unbox_be.order.repository.AdminOrderRepository;
import com.example.unbox_be.order.dto.request.OrderStatusUpdateRequestDto;
import com.example.unbox_be.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.order.dto.response.OrderResponseDto;
import com.example.unbox_be.order.entity.Order;
import com.example.unbox_be.order.mapper.OrderMapper;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderServiceImpl implements AdminOrderService {

    private final AdminOrderRepository adminOrderRepository;
    private final OrderMapper orderMapper;

    // ✅ 관리자 주문 목록 조회 (검색 + 페이징)
    @Override
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public Page<OrderResponseDto> getAdminOrders(OrderSearchCondition condition, Pageable pageable) {
        Page<Order> orders = adminOrderRepository.findAdminOrders(condition, pageable);
        return orders.map(orderMapper::toResponseDto);
    }

    // ✅ 관리자 주문 상세 조회
    @Override
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public OrderDetailResponseDto getAdminOrderDetail(UUID orderId) {
        Order order = adminOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return orderMapper.toDetailResponseDto(order);
    }

    // ✅ 관리자 주문 상태 변경
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MASTER','MANAGER')")
    public OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatusUpdateRequestDto requestDto) {
        Order order = adminOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        order.updateAdminStatus(
                requestDto.getStatus(),
                requestDto.getTrackingNumber()
        );

        return orderMapper.toDetailResponseDto(order);
    }
}