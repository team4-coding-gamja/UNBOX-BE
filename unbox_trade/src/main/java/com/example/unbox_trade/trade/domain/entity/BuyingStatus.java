package com.example.unbox_trade.trade.domain.entity;

public enum BuyingStatus {

    LIVE,
    // 구매 입찰 활성 상태
    // 판매 입찰과 매칭 가능

    RESERVED,
    // 판매 입찰과 매칭되어 예약된 상태 (기존 MATCHED)
    // 주문 생성 또는 결제 대기 단계

    SOLD,
    // 거래 완료 (기존 COMPLETED)
    // 주문 및 결제가 성공적으로 완료된 상태

    CANCELLED,
    // 구매자가 입찰을 취소한 상태

    EXPIRED;
    // 입찰 기한 만료
}