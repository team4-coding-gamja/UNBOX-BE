package com.example.unbox_be.order.settlement.entity;

public enum SettlementStatus {

    PENDING, // 정산 대기
    // 결제 완료 후 정산 대기 상태
    // 구매 확정 전까지 유지

    CONFIRMED, // 정산 완료
    // 구매 확정 완료
    // 판매자에게 지급 가능한 상태

    CANCELLED, // 정산 취소
    // 환불, 검수 불합격 등으로
    // 정산이 무효 처리된 상태

    ON_HOLD; // 정산 보류
    // 분쟁, 클레임, 운영자 개입 등으로
    // 정산이 일시 보류된 상태
}

