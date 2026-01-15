package com.example.unbox_be.domain.order.order.entity;

public enum OrderStatus {
    PENDING_SHIPMENT,       // 발송 대기
    SHIPPED_TO_CENTER,      // 판매자가 운송장 등록 -> 센터로 출발
    ARRIVED_AT_CENTER,      // 센터 도착
    IN_INSPECTION,          // 검수 중
    INSPECTION_PASSED,      // 검수 합격
    INSPECTION_FAILED,      // 검수 불합격
    SHIPPED_TO_BUYER,       // 구매자에게 배송 중
    DELIVERED,              // 배송 완료 (구매자 수령)
    COMPLETED,              // 거래 완료
    CANCELLED               // 취소됨
}