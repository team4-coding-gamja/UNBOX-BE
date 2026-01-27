package com.example.unbox_payment.payment.domain.entity;

public enum PaymentStatus {
    READY, // 결제 요청 대기
    IN_PROGRESS, // 결제 승인 중 (중복 요청 방지)
    DONE, // 결제 완료 (captured)
    REFUND_IN_PROGRESS, // 환불 처리 중 (중복 요청 방지)
    CANCELED, // 결제 취소
    FAILED // 결제 실패
}