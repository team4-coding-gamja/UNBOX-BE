package com.example.unbox_payment.test;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_payment.payment.domain.entity.Payment;
import com.example.unbox_payment.payment.domain.entity.PaymentMethod;
import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import com.example.unbox_payment.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * loadtest 전용: markAsInProgress 를 REQUIRES_NEW 트랜잭션으로 분리하는 빈
 * - Spring 프록시 self-call 문제를 해결하기 위해 별도 빈으로 분리
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Profile("loadtest")
public class TestPaymentStatusUpdater {

    private final PaymentRepository paymentRepository;

    /**
     * loadtest 전용 상태 준비:
     * - paymentId가 없으면 테스트 기본값으로 생성
     * - 어떤 상태든 READY로 재정렬 후 IN_PROGRESS로 전이
     * - DB seed/reset 없이 k6 재실행 가능하도록 설계
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markAsInProgressInNewTx(Long userId, UUID paymentId, BigDecimal amountFromFront) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseGet(() -> createLoadtestPayment(userId, paymentId, amountFromFront));

        if (payment.getAmount().compareTo(amountFromFront) != 0) {
            throw new CustomException(ErrorCode.AMOUNT_MISMATCH);
        }

        if (payment.getStatus() != PaymentStatus.READY || payment.isExpired()) {
            payment.markAsReady();
        }
        payment.changeStatus(PaymentStatus.IN_PROGRESS);
        paymentRepository.saveAndFlush(payment);

        log.debug("[TestPaymentStatusUpdater] IN_PROGRESS committed - paymentId: {}", paymentId);
        return payment;
    }

    private Payment createLoadtestPayment(Long userId, UUID paymentId, BigDecimal amountFromFront) {
        long resolvedUserId = userId != null ? userId : 1L;

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(uuidFrom("order-" + paymentId))
                .sellingBidId(uuidFrom("sell-" + paymentId))
                .buyerId(resolvedUserId)
                .sellerId(1000L + resolvedUserId)
                .method(PaymentMethod.CARD)
                .amount(amountFromFront)
                .status(PaymentStatus.READY)
                .paymentKey("test_success_" + paymentId)
                .readyAt(LocalDateTime.now())
                .version(0L)
                .build();

        return paymentRepository.saveAndFlush(payment);
    }

    private UUID uuidFrom(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
