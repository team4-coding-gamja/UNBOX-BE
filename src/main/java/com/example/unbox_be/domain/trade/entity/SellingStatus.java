package com.example.unbox_be.domain.trade.entity;

public enum SellingStatus {
    LIVE, // 판매 입찰 활성 상태
    HOLD, // 일시 중지(처리/중복 방지)
    MATCHED, // 구매 입찰과 매칭 완료
    CANCELLED // 판매 입찰 취소
}