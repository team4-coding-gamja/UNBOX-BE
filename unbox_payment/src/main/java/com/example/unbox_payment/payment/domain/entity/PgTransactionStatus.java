package com.example.unbox_payment.payment.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PgTransactionStatus {
    READY("결제 생성"),
    IN_PROGRESS("결제 진행 중"),
    WAITING_FOR_DEPOSIT("입금 대기"),
    DONE("결제 완료"),
    CANCELED("결제 취소"),
    PARTIAL_CANCELED("부분 취소"),
    FAILED("결제 승인 실패"),
    EXPIRED("유효 시간 만료");

    private final String description;
}