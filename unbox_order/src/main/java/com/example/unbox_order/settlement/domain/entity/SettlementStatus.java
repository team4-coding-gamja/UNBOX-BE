package com.example.unbox_order.settlement.domain.entity;

public enum SettlementStatus {

    PENDING, // 정산 대기
    // 결제 완료 후 정산 대기 상태
    // 토스 정산 데이터 생성 전 (D-Day)

    SETTLEMENT_READY, // 정산 가능
    // 토스 정산 데이터 생성 완료 (D+1)
    // soldDate, paidOutDate 결정됨

    SETTLEMENT_SCHEDULED, // 정산 예정
    // 정산 지급일 도래 대기 중
    // paidOutDate 이전 상태

    PAID_OUT, // 지급 완료
    // 판매자 계좌로 실제 입금 완료
    // completedAt 기록됨

    CANCELLED, // 정산 취소
    // 환불, 검수 불합격 등으로
    // 정산이 무효 처리된 상태

    ON_HOLD // 정산 보류
    // 분쟁, 클레임, 운영자 개입 등으로
    // 정산이 일시 보류된 상태
}
