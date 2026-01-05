package com.example.unbox_be.domain.payment.entity;

public enum PaymentStatus {
    READY,   // 결제 요청 대기
    DONE,    // 결제 완료 (captured)
    CANCELED, // 결제 취소
    FAILED   // 결제 실패
}