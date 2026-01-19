package com.example.unbox_be.domain.payment.repository;


import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentMethod;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.global.config.JpaAuditingConfig;
import com.example.unbox_be.global.config.TestQueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager; // 데이터 적재를 위한 유틸리티

    private UUID orderId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        payment = Payment.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(10000))
                .status(PaymentStatus.READY)
                .method(PaymentMethod.CARD)
                .build();

        entityManager.persist(payment);
        entityManager.flush();
    }

    @Test
    @DisplayName("1. 성공 - ID와 삭제되지 않은 조건으로 결제 조회")
    void findByIdAndDeletedAtIsNull_success() {
        // when
        Optional<Payment> result = paymentRepository.findByIdAndDeletedAtIsNull(payment.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("2. 실패 - 데이터는 존재하지만 삭제된(deletedAt 존재) 경우 조회되지 않음")
    void findByIdAndDeletedAtIsNull_fail_deleted() {
        // given
        ReflectionTestUtils.setField(payment, "deletedAt", LocalDateTime.now());
        entityManager.flush();

        // when
        Optional<Payment> result = paymentRepository.findByIdAndDeletedAtIsNull(payment.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("3. 성공 - 주문 ID로 삭제되지 않은 결제 내역 조회")
    void findByOrderIdAndDeletedAtIsNull_success() {
        // when
        Optional<Payment> result = paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("4. 성공 - 주문 ID로 결제 내역 존재 여부 확인")
    void existsByOrderIdAndDeletedAtIsNull_success() {
        // when
        boolean exists = paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("5. 성공 - PG 결제 키(paymentKey)로 결제 내역 조회")
    void findByPgPaymentKeyAndDeletedAtIsNull_success() {
        // given
        // setUp에서 pgPaymentKey가 저장된 '완료(DONE)' 상태의 데이터를 미리 준비해야 합니다.
        String targetKey = "toss_real_key_123";
        payment.completePayment(targetKey, "APP_123456");
        entityManager.flush();
        entityManager.clear();

        // when
        // 레포지토리 메서드명도 바뀐 필드명에 맞춰 수정되어야 합니다.
        Optional<Payment> result = paymentRepository.findByPgPaymentKeyAndDeletedAtIsNull(targetKey);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPgPaymentKey()).isEqualTo(targetKey);
        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("6. 성공 - 특정 주문의 DONE 상태 결제 조회")
    void findDonePaymentByOrderId_success() {
        // given: 상태를 DONE으로 변경
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.DONE);
        entityManager.flush();

        // when
        Optional<Payment> result = paymentRepository.findDonePaymentByOrderId(orderId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("7. 실패 - 주문 ID는 맞지만 상태가 DONE이 아닌 경우 조회 실패")
    void findDonePaymentByOrderId_fail_statusMismatch() {
        // given: 상태가 READY (DONE이 아님)
        // when
        Optional<Payment> result = paymentRepository.findDonePaymentByOrderId(orderId);

        // then
        assertThat(result).isEmpty();
    }
}
