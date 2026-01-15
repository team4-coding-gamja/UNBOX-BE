package com.example.unbox_be.domain.order.order.service;

import com.example.unbox_be.domain.order.order.dto.request.OrderCreateRequestDto;
import com.example.unbox_be.domain.order.order.dto.response.OrderDetailResponseDto;
import com.example.unbox_be.domain.order.order.dto.response.OrderResponseDto;
import com.example.unbox_be.domain.order.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    /**
     * 주문 생성
     * @param requestDto 주문 요청 정보
     * @param buyerId 구매자 ID (PK)
     * @return 생성된 주문의 UUID
     */
    UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId);

    /**
     * 내 주문 목록 조회 (페이징)
     * @param buyerId 구매자 ID (PK)
     * @param pageable 페이징 정보
     * @return 주문 목록 Page 객체
     */
    Page<OrderResponseDto> getMyOrders(Long buyerId, Pageable pageable);

    /**
     * 주문 상세 조회
     * @param orderId 주문 UUID
     * @param userId 요청자 ID (본인 확인용)
     * @return 주문 상세 정보
     */
    OrderDetailResponseDto getOrderDetail(UUID orderId, Long userId);

    /**
     * 주문 취소 (배송 전)
     * @param orderId 주문 UUID
     * @param userId 요청자 ID (구매자 본인 확인용)
     * @return 취소된 주문 정보
     */
    OrderDetailResponseDto cancelOrder(UUID orderId, Long userId);

    /**
     * 운송장 번호 등록 (판매자용)
     * @param orderId 주문 UUID
     * @param trackingNumber 운송장 번호
     * @param sellerId 판매자 ID (본인 확인용)
     * @return 업데이트된 주문 정보
     */
    OrderDetailResponseDto registerTracking(UUID orderId, String trackingNumber, Long sellerId);

    /**
     * 주문 상태 변경 (관리자/검수자용)
     * @param orderId 주문 UUID
     * @param newStatus 변경할 상태
     * @param finalTrackingNumber 최종 운송장 번호 (센터 -> 구매자, 필요 시)
     * @param adminId 관리자 ID (권한 확인용)
     * @return 업데이트된 주문 정보
     */
    OrderDetailResponseDto updateAdminStatus(UUID orderId, OrderStatus newStatus, String finalTrackingNumber, Long adminId);

    /**
     * 구매 확정 (구매자용)
     * @param orderId 주문 UUID
     * @param userId 구매자 ID (본인 확인용)
     * @return 확정된 주문 정보
     */
    OrderDetailResponseDto confirmOrder(UUID orderId, Long userId);
}
