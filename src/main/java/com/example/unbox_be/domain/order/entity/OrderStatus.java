package com.example.unbox_be.domain.order.entity;

public enum OrderStatus {
    PENDING_SHIPMENT,       // 발송 대기
    ARRIVED_AT_CENTER,      // 센터 도착
    IN_INSPECTION,          // 검수 중
    INSPECTION_PASSED,      // 검수 합격
    INSPECTION_FAILED,      // 검수 불합격
    SHIPPED_TO_BUYER,       // 배송 중
    COMPLETED,              // 거래 완료
    CANCELLED               // 취소됨
}