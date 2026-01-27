package com.example.unbox_payment.payment.service;

import com.example.unbox_payment.payment.dto.internal.PaymentForSettlementResponse;
import com.example.unbox_payment.payment.dto.internal.PaymentStatusResponse;
import com.example.unbox_payment.payment.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    // ✅ 결계 이력 조회
    List<PaymentHistoryResponseDto> getPaymentHistory(Long userId);

    // ✅ 결제 준비 (초기 레코드 생성)
    PaymentReadyResponseDto createPayment(Long userId, UUID orderId, PaymentMethod method);

    // ✅ 결제 승인 처리
    TossConfirmResponse confirmPayment(Long userId, UUID paymentId, String paymentKey, BigDecimal amount);

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    /**
     * 결제 조회 (정산용)
     */
    PaymentForSettlementResponse getPaymentForSettlement(UUID paymentId);

    /**
     * 결제 상태 조회
     */
    PaymentStatusResponse getPaymentStatus(UUID orderId);

    /**
     * 환불 처리 (결제 취소)
     * 
     * @param paymentId   결제 ID
     * @param reason      취소 사유
     */
    void processRefund(UUID paymentId, String reason);
}