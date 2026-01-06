package com.example.unbox_be.domain.admin.order.service;

import com.example.unbox_be.domain.admin.order.dto.OrderSearchCondition;
import com.example.unbox_be.domain.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminOrderService {
    /**
     * 전체 주문 목록 조회 (검색 조건 포함)
     * @param condition 검색 조건
     * @param pageable 페이징 정보
     * @return 주문 목록 Page 객체
     */
    Page<OrderResponseDto> getAdminOrders(OrderSearchCondition condition, Pageable pageable);

    /**
     * 주문 상세 조회 (관리자용)
     * @param orderId 주문 UUID
     * @param adminId 관리자 ID
     * @return 주문 상세 정보
     */
    OrderDetailResponseDto getAdminOrderDetail(UUID orderId, Long adminId);

    /**
     * 주문 상태 변경 (관리자/검수자용)
     * @param orderId 주문 UUID
     * @param newStatus 변경할 상태
     * @param finalTrackingNumber 최종 운송장 번호 (센터 -> 구매자, 필요 시)
     * @param adminId 관리자 ID (권한 확인용)
     * @return 업데이트된 주문 정보
     */
    OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatus newStatus, String finalTrackingNumber, Long adminId);
}
