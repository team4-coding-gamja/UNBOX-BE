package com.example.unbox_be.domain.admin.order.service;

import com.example.unbox_be.domain.admin.common.entity.Admin;
import com.example.unbox_be.domain.admin.common.entity.AdminRole;
import com.example.unbox_be.domain.admin.common.repository.AdminRepository;
import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.admin.order.repository.OrderAdminRepository;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.mapper.OrderMapper;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderAdminRepository orderAdminRepository;
    private final AdminRepository adminRepository;
    private final OrderMapper orderMapper;

    @Override
    public Page<OrderResponseDto> getAdminOrders(OrderSearchCondition condition, Pageable pageable) {
        Page<Order> orders = orderAdminRepository.findAdminOrders(condition, pageable);
        return orders.map(orderMapper::toResponseDto);
    }

    @Override
    public OrderDetailResponseDto getAdminOrderDetail(UUID orderId, Long adminId) {
        // 관리자 존재 여부 확인
        if (!adminRepository.existsById(adminId)) {
            throw new CustomException(ErrorCode.ADMIN_NOT_FOUND);
        }

        Order order = getOrderWithDetailsOrThrow(orderId);
        return orderMapper.toDetailResponseDto(order);
    }

    @Override
    @Transactional
    public OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatus newStatus, String finalTrackingNumber, Long adminId) {
        // 1. 관리자 조회
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.ADMIN_NOT_FOUND));

        // 2. 권한 검증 (MASTER, INSPECTOR만 가능)
        if (admin.getAdminRole() != AdminRole.ROLE_MASTER && admin.getAdminRole() != AdminRole.ROLE_INSPECTOR) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 주문 조회
        Order order = getOrderWithDetailsOrThrow(orderId);

        // 4. 도메인 로직 호출 (Entity 내부에서 상태 전이 검증 수행)
        order.updateAdminStatus(newStatus, finalTrackingNumber);

        return orderMapper.toDetailResponseDto(order);
    }

    private Order getOrderWithDetailsOrThrow(UUID orderId) {
        return orderAdminRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }
}
