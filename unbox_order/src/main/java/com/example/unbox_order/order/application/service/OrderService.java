package com.example.unbox_order.order.application.service;

import com.example.unbox_order.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_order.common.client.order.dto.OrderForReviewInfoResponse;
import com.example.unbox_order.order.presentation.dto.request.OrderCreateRequestDto;
import com.example.unbox_order.order.presentation.dto.response.OrderDetailResponseDto;
import com.example.unbox_order.order.presentation.dto.response.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    /**
     * 주문 생성
     * 
     * @param requestDto 주문 요청 정보
     * @param buyerId    구매자 ID (PK)
     * @return 생성된 주문의 UUID
     */
    UUID createOrder(OrderCreateRequestDto requestDto, Long buyerId);

    /**
     * 내 주문 목록 조회 (페이징)
     * 
     * @param buyerId  구매자 ID (PK)
     * @param pageable 페이징 정보
     * @return 주문 목록 Page 객체
     */
    Page<OrderResponseDto> getMyOrders(Long buyerId, Pageable pageable);

    /**
     * 주문 상세 조회
     * 
     * @param orderId 주문 UUID
     * @param userId  요청자 ID (본인 확인용)
     * @return 주문 상세 정보
     */
    OrderDetailResponseDto getOrderDetail(UUID orderId, Long userId);

    /**
     * 주문 취소 (배송 전)
     * 
     * @param orderId 주문 UUID
     * @param userId  요청자 ID (구매자 본인 확인용)
     * @return 취소된 주문 정보
     */
    OrderDetailResponseDto cancelOrder(UUID orderId, Long userId);

    /**
     * 운송장 번호 등록 (판매자용)
     * 
     * @param orderId        주문 UUID
     * @param trackingNumber 운송장 번호
     * @param sellerId       판매자 ID (본인 확인용)
     * @return 업데이트된 주문 정보
     */
    OrderDetailResponseDto registerTracking(UUID orderId, String trackingNumber, Long sellerId);

    /**
     * 구매 확정 (구매자용)
     * 
     * @param orderId 주문 UUID
     * @param userId  구매자 ID (본인 확인용)
     * @return 확정된 주문 정보
     */
    OrderDetailResponseDto confirmOrder(UUID orderId, Long userId);

    /**
     * 환불 요청 (결제 후, 구매자만)
     * PENDING_SHIPMENT 또는 DELIVERED 상태에서만 가능
     * 
     * @param orderId 주문 UUID
     * @param reason  취소 사유
     * @param userId  구매자 ID (본인 확인용)
     * @return 취소된 주문 정보
     */
    OrderDetailResponseDto requestRefund(UUID orderId, String reason, Long userId);

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================
    /**
     * 주문 조회 (리뷰용)
     */
    OrderForReviewInfoResponse getOrderForReview(UUID orderId);

    /**
     * 주문 조회 (결제용)
     */
    OrderForPaymentInfoResponse getOrderForPayment(UUID orderId);

    /**
     * 주문 상태 변경 (결제 완료용: PAYMENT_PENDING → PENDING_SHIPMENT)
     */
    void pendingShipmentOrder(UUID orderId, UUID paymentId, String updatedBy);

    // ========================================
    // ✅ 검수 시스템 연동 (Inspection System Integration)
    // ========================================

    void startInspection(UUID orderId);

    void passedInspection(UUID orderId);

    void failedInspection(UUID orderId);
}
