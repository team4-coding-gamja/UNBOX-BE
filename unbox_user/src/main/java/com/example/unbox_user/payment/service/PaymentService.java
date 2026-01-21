package com.example.unbox_user.payment.service;

import com.example.unbox_user.common.client.payment.dto.PaymentForSettlementResponse;
import com.example.unbox_user.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_user.payment.dto.response.TossConfirmResponse;
import com.example.unbox_user.payment.entity.PaymentMethod;

import java.util.UUID;

public interface PaymentService {

    // ✅ 결제 준비 (초기 레코드 생성)
    PaymentReadyResponseDto createPayment(Long userId, UUID orderId, PaymentMethod method);

    // ✅ 결제 승인 처리
    TossConfirmResponse confirmPayment(Long userId, UUID paymentId, String paymentKey);

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    /**
     * 결제 조회 (정산용)
     */
    PaymentForSettlementResponse getPaymentForSettlement(UUID paymentId);
}