package com.example.unbox_payment.test;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_payment.payment.domain.entity.Payment;
import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import com.example.unbox_payment.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestPaymentStatusTransitionService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment markAsInProgress(UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.READY) {
            throw new CustomException(ErrorCode.PAYMENT_IN_PROGRESS);
        }

        payment.changeStatus(PaymentStatus.IN_PROGRESS);
        paymentRepository.saveAndFlush(payment);

        log.debug("[TestPaymentPrepare] IN_PROGRESS committed - paymentId: {}", paymentId);
        return payment;
    }
}
