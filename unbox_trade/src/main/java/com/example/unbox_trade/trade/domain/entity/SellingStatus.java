package com.example.unbox_trade.trade.domain.entity;

public enum SellingStatus {

    LIVE, // 판매 중
    // 판매 입찰 활성 상태
    // 구매자가 선택하여 주문을 생성할 수 있는 상태

    RESERVED, // 예약 중
    // 주문 생성 또는 결제 대기 중인 상태 (선점됨)
    // 동시성 방지를 위해 다른 구매자는 선택 불가
    // 결제 실패/취소/TTL 만료 시 다시 LIVE로 복구됨

    SOLD, // 판매 완료
    // 결제 성공으로 거래가 확정된 상태
    // 더 이상 구매 불가, 이후 판매자 발송 프로세스로 진행

    CANCELLED; // 판매 취소
    // 판매자가 직접 취소하거나
    // 관리자에 의해 입찰이 종료된 상태
}