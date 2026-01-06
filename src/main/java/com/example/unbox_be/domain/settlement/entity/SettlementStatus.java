package com.example.unbox_be.domain.settlement.entity;

public enum SettlementStatus {
    WAITING, // 정산 대기 -> 결제 직후, 구매 확정까지
    DONE, // 정산 완료 -> 구매 확정 끝남
    HOLD // 정산 보류 -> 반품 or 예외 상황 발생
}
