package com.example.unbox_common.event.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 후 주문 취소(환불) 요청 이벤트
 * - 배송 대기(PENDING_SHIPMENT) 또는 배송 완료(DELIVERED) 상태에서 구매자가 취소 시 발행
 * - Consumer: Payment(환불 처리), Trade(입찰 상태 복구)
 */
public record OrderRefundRequestedEvent(
    UUID orderId,
    UUID sellingBidId,
    UUID paymentId,           // 환불 대상 결제
    Long buyerId,
    Long sellerId,
    BigDecimal refundAmount,   // 환불 금액
    String previousStatus,     // PENDING_SHIPMENT or DELIVERED
    String reason,             // 취소 사유
    LocalDateTime requestedAt
) {
    public static OrderRefundRequestedEvent of(
            UUID orderId,
            UUID sellingBidId,
            UUID paymentId,
            Long buyerId,
            Long sellerId,
            BigDecimal refundAmount,
            String previousStatus,
            String reason
    ) {
        return new OrderRefundRequestedEvent(
                orderId,
                sellingBidId,
                paymentId,
                buyerId,
                sellerId,
                refundAmount,
                previousStatus,
                reason,
                LocalDateTime.now()
        );
    }
}
