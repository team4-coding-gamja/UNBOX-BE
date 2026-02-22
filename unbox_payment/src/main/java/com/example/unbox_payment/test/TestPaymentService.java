package com.example.unbox_payment.test;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_payment.test.client.TestOrderClient;
import com.example.unbox_payment.payment.domain.entity.Payment;
import com.example.unbox_payment.payment.presentation.dto.response.TossConfirmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestPaymentService {

    private static final String UPDATED_BY = "payment-service-test";
    private static final String FAULT_TARGET_ORDER = "order";

    private final TestPaymentPreparationService testPaymentPreparationService;
    private final TestOrderClient testOrderClient;

    // 동기 구조: 결제 완료 후 주문 상태 변경을 동기 호출로 수행
    public TossConfirmResponse confirmPaymentSync(
            Long userId,
            UUID paymentId,
            String paymentKeyFromFront,
            BigDecimal amountFromFront,
            String faultTarget,
            long faultDelayMs) {
        log.info(
                "[TestPaymentConfirm][SYNC] start - paymentId: {}, userId: {}, faultTarget: {}, faultDelayMs: {}",
                paymentId, userId, faultTarget, faultDelayMs);

        Payment payment = testPaymentPreparationService.prepareForConfirmWithoutOrderLookup(
                userId,
                paymentId,
                amountFromFront);
        String finalPaymentKey = normalizePaymentKey(paymentKeyFromFront, paymentId);
        TossConfirmResponse mockResponse = buildMockResponse(payment, finalPaymentKey);

        testPaymentPreparationService.completeForConfirmWithoutPgLog(paymentId, finalPaymentKey);

        long normalizedDelay = Math.max(faultDelayMs, 0L);
        String normalizedFaultTarget = isOrderFaultTarget(faultTarget) ? FAULT_TARGET_ORDER : "";

        try {
            testOrderClient.pendingShipmentOrder(
                    payment.getOrderId(),
                    paymentId,
                    UPDATED_BY,
                    normalizedFaultTarget,
                    normalizedDelay);
        } catch (Exception e) {
            log.error("[TestPaymentConfirm][SYNC] pending-shipment failed - paymentId: {}", paymentId, e);
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        log.info("[TestPaymentConfirm][SYNC] done - paymentId: {}", paymentId);
        return mockResponse;
    }

    // 비동기 구조: 결제 완료 응답 시점에는 다운스트림(Order/Trade/Settlement) 호출을 수행하지 않음
    public TossConfirmResponse confirmPaymentAsync(
            Long userId,
            UUID paymentId,
            String paymentKeyFromFront,
            BigDecimal amountFromFront) {
        log.info("[TestPaymentConfirm][ASYNC] start - paymentId: {}, userId: {}", paymentId, userId);

        Payment payment = testPaymentPreparationService.prepareForConfirmWithoutOrderLookup(
                userId,
                paymentId,
                amountFromFront);
        String finalPaymentKey = normalizePaymentKey(paymentKeyFromFront, paymentId);
        TossConfirmResponse mockResponse = buildMockResponse(payment, finalPaymentKey);

        testPaymentPreparationService.completeForConfirmWithoutPgLog(paymentId, finalPaymentKey);

        log.info(
                "[TestPaymentConfirm][ASYNC] done without downstream call - paymentId: {}",
                paymentId);
        return mockResponse;
    }

    private TossConfirmResponse buildMockResponse(Payment payment, String paymentKey) {
        return TossConfirmResponse.builder()
                .paymentKey(paymentKey)
                .orderId(payment.getOrderId().toString())
                .totalAmount(payment.getAmount())
                .method("CARD")
                .status("DONE")
                .approvedAt(LocalDateTime.now().toString())
                .build();
    }

    private String normalizePaymentKey(String paymentKeyFromFront, UUID paymentId) {
        if (paymentKeyFromFront == null || paymentKeyFromFront.isBlank()) {
            return "test_success_" + paymentId;
        }
        if (paymentKeyFromFront.startsWith("test_success_")) {
            return paymentKeyFromFront;
        }
        return "test_success_" + paymentKeyFromFront;
    }

    private boolean isOrderFaultTarget(String faultTarget) {
        return FAULT_TARGET_ORDER.equalsIgnoreCase(faultTarget);
    }
}
