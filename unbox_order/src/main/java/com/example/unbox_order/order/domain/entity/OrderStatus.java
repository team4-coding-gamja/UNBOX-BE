package com.example.unbox_order.order.entity;

public enum OrderStatus {

    PAYMENT_PENDING, // 결제 대기
    // 주문 생성 완료, 결제 대기 상태
    // 판매 입찰은 RESERVED 상태
    // 결제 실패/취소/TTL 만료 시 CANCELLED 로 종료

    PENDING_SHIPMENT, // 배송 대기
    // 결제 완료 상태
    // 판매자 발송 대기 (검수 센터로 1차 배송 전)

    SHIPPED_TO_CENTER, // 센터 배송 중
    // 판매자가 운송장을 입력하고
    // 검수 센터로 배송 중인 상태

    ARRIVED_AT_CENTER, // 배송 도착
    // 검수 센터에 상품이 도착한 상태

    IN_INSPECTION, // 검수 중
    // 검수 진행 중인 상태

    INSPECTION_PASSED, // 검수 합격
    // 검수 합격
    // 구매자에게 배송 준비 단계

    INSPECTION_FAILED, // 겁수 불합격
    // 검수 불합격
    // 주문 취소 및 환불 처리로 이동

    SHIPPED_TO_BUYER, // 구매자 배송 중
    // 검수 센터에서 구매자에게 배송 중

    DELIVERED, // 배송 도착
    // 구매자에게 배송 완료
    // 구매 확정 또는 클레임 가능 구간

    COMPLETED, // 구매 확정
    // 구매 확정 완료
    // 거래 종료 및 정산 확정

    CANCELLED; // 주문 취소
    // 결제 실패, 판매자 미발송, 검수 불합격 등으로
    // 주문이 종료된 상태
}