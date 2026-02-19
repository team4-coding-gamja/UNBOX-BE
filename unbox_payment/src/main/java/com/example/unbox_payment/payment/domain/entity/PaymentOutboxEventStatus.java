package com.example.unbox_payment.payment.domain.entity;

public enum PaymentOutboxEventStatus {

    PENDING, // 발행 대기 중

    PROCESSING, // 발행 중

    PUBLISHED, // 발행 완료

    FAILED // 발행 실패
}
