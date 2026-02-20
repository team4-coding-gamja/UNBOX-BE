package com.example.unbox_payment.test;

import com.example.unbox_payment.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("loadtest")
public class TestPaymentPreparationService {

    private final TestPaymentStatusUpdater statusUpdater;

    /**
     * loadtest 전용 prepare 단계:
     * - Order 조회를 생략
     * - 결제 레코드가 없어도 테스트 전용 기본값으로 자동 생성
     * - DONE/IN_PROGRESS 등 상태는 테스트 재실행을 위해 READY로 재정렬 후 IN_PROGRESS 상태 전이
     */
    public Payment prepareForConfirmWithoutOrderLookup(Long userId, UUID paymentId, BigDecimal amountFromFront) {
        return statusUpdater.markAsInProgressInNewTx(userId, paymentId, amountFromFront);
    }
}
