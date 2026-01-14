//package com.example.unbox_be.domain.payment.repository;
//
//
//import com.example.unbox_be.domain.payment.entity.Payment;
//import com.example.unbox_be.domain.payment.entity.PaymentMethod;
//import com.example.unbox_be.domain.payment.entity.PaymentStatus;
//import com.example.unbox_be.global.config.JpaAuditingConfig;
//import com.example.unbox_be.global.config.TestQueryDslConfig;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.orm.ObjectOptimisticLockingFailureException;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.TransactionDefinition;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//@DataJpaTest
//@ActiveProfiles("test")
//@Import({JpaAuditingConfig.class, TestQueryDslConfig.class})
//@TestPropertySource(properties = {"spring.sql.init.mode=never"})
//class PaymentRepositoryTest {
//
//    @Autowired
//    private PaymentRepository paymentRepository;
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    private TransactionTemplate transactionTemplate;
//    @Autowired
//    private PlatformTransactionManager transactionManager;
//
//    private UUID sampleOrderId;
//
//    @BeforeEach
//    void setUp() {
//        sampleOrderId = UUID.randomUUID();
//    }
//
//    @Test
//    @DisplayName("낙관적 락: 동일한 엔티티를 동시에 수정하면 예외가 발생한다")
//    void optimisticLock_ConcurrencyExceptionTest() {
//        TransactionTemplate tt = new TransactionTemplate(transactionManager);
//        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//
//// 2. [트랜잭션 1] 초기 데이터 생성 및 커밋 (version 0)
//        UUID paymentId = tt.execute(status -> {
//            Payment payment = Payment.builder()
//                    .orderId(sampleOrderId)
//                    .amount(BigDecimal.valueOf(50000))
//                    .method(PaymentMethod.CARD)
//                    .status(PaymentStatus.READY)
//                    .build();
//            return paymentRepository.save(payment).getId();
//        });
//
//        // 3. [트랜잭션 2] 사용자 A가 조회 (version 0)
//        Payment userA = tt.execute(status -> paymentRepository.findById(paymentId).get());
//
//        // 4. [트랜잭션 3] 사용자 B가 조회 (version 0)
//        Payment userB = tt.execute(status -> paymentRepository.findById(paymentId).get());
//
//        // 5. [트랜잭션 4] 사용자 A가 먼저 수정 완료 및 커밋 (version: 0 -> 1)
//        tt.execute(status -> {
//            userA.completePayment("key_A", "app_A");
//            paymentRepository.saveAndFlush(userA);
//            return null;
//        });
//
//        // 6. [트랜잭션 5] 사용자 B가 수정 시도 (실패해야 함)
//        // userB는 여전히 메모리에 version 0을 들고 있음
//        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
//            tt.execute(status -> {
//                // 비즈니스 로직 검증을 피하기 위해 리플렉션으로 필드 수정 (상태체크 우회)
//                org.springframework.test.util.ReflectionTestUtils.setField(userB, "pgPaymentKey", "key_B");
//                paymentRepository.saveAndFlush(userB); // 여기서 version 충돌 발생!
//                return null;
//            });
//        });
//    }
//    @Test
//    @DisplayName("낙관적 락: DB의 현재 버전보다 낮은 버전으로 수정을 시도하면 실패해야 한다")
//    void optimisticLock_LowerVersionUpdateTest() {
//        TransactionTemplate tt = new TransactionTemplate(transactionManager);
//        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        // 1. 초기 데이터 생성 (version 0)
//        UUID paymentId = tt.execute(status -> {
//            Payment payment = Payment.builder()
//                    .orderId(sampleOrderId)
//                    .amount(BigDecimal.valueOf(10000))
//                    .method(PaymentMethod.CARD)
//                    .status(PaymentStatus.READY)
//                    .build();
//            return paymentRepository.save(payment).getId();
//        });
//
//        // 2. 외부에서 데이터를 미리 수정하여 버전을 올림 (version: 0 -> 1 -> 2)
//        tt.execute(status -> {
//            Payment p = paymentRepository.findById(paymentId).get();
//            p.completePayment("key1", "app1"); // version 1
//            paymentRepository.saveAndFlush(p);
//
//            p.changeStatus(PaymentStatus.CANCELED); // version 2
//            paymentRepository.saveAndFlush(p);
//            return null;
//        });
//
//        // 3. [상황] 사용자 B는 아주 예전 버전(version 0)인 줄 알고 수정을 시도함
//        Payment userB = tt.execute(status -> paymentRepository.findById(paymentId).get());
//        // 강제로 객체의 버전을 과거(0)로 돌려버림 (오래된 캐시 상황 재현)
//        org.springframework.test.util.ReflectionTestUtils.setField(userB, "version", 0L);
//
//        // then: 현재 DB 버전은 2인데, 0인 객체를 저장하려고 하면 당연히 실패해야 함
//        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
//            tt.execute(status -> {
//                org.springframework.test.util.ReflectionTestUtils.setField(userB, "pgApproveNo", "wrong_update");
//                paymentRepository.saveAndFlush(userB);
//                return null;
//            });
//        });
//    }
//    @Test
//    @DisplayName("낙관적 락: 수정이 성공할 때마다 버전 번호는 순차적으로 1씩 증가해야 한다")
//    void optimisticLock_VersionSequenceTest() {
//        TransactionTemplate tt = new TransactionTemplate(transactionManager);
//        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
//        // 1. 초기 데이터 생성 (version 0)
//        UUID paymentId = tt.execute(status -> {
//            Payment payment = Payment.builder()
//                    .orderId(sampleOrderId)
//                    .amount(BigDecimal.valueOf(20000))
//                    .method(PaymentMethod.CARD)
//                    .status(PaymentStatus.READY)
//                    .build();
//            return paymentRepository.save(payment).getId();
//        });
//
//        // 2. 세 번의 독립적인 수정 트랜잭션 실행
//        for (int i = 1; i <= 3; i++) {
//            final int currentStep = i;
//            tt.execute(status -> {
//                Payment p = paymentRepository.findById(paymentId).get();
//                // 각 단계마다 다른 필드 수정
//                org.springframework.test.util.ReflectionTestUtils.setField(p, "pgApproveNo", "APP_" + currentStep);
//                paymentRepository.saveAndFlush(p);
//                return null;
//            });
//        }
//
//        // 3. 최종 버전 확인
//        Payment finalPayment = paymentRepository.findById(paymentId).get();
//
//        // 처음 0에서 시작해서 3번 수정되었으므로 최종 버전은 3이어야 함
//        assertThat(finalPayment.getVersion()).isEqualTo(3L);
//    }
//    // --- 2. Query Method 및 Custom Query 테스트 ---
//
//    @Test
//    @DisplayName("주문 ID로 결제 내역 조회 - 삭제된 데이터는 제외되어야 한다")
//    void findByOrderIdAndDeletedAtIsNull_Test() {
//
//        // given
//        Payment payment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(10000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.READY)
//                .build();
//        paymentRepository.saveAndFlush(payment);
//
//        // when
//        Optional<Payment> found = paymentRepository.findByOrderIdAndDeletedAtIsNull(sampleOrderId);
//
//        // then
//        assertThat(found).isPresent();
//        assertThat(found.get().getOrderId()).isEqualTo(sampleOrderId);
//    }
//
//    @Test
//    @DisplayName("PG 승인 번호로 결제 내역을 조회할 수 있다")
//    void findByPgPaymentKey_Test() {
//        // given
//        String targetKey = "toss_test_key_123";
//        Payment payment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(20000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.DONE)
//                .pgPaymentKey(targetKey)
//                .build();
//        paymentRepository.saveAndFlush(payment);
//
//        // when
//        Optional<Payment> found = paymentRepository.findByPgPaymentKeyAndDeletedAtIsNull(targetKey);
//
//        // then
//        assertThat(found).isPresent();
//        assertThat(found.get().getPgPaymentKey()).isEqualTo(targetKey);
//    }
//    @Test
//    @DisplayName("Soft Delete 검증: 삭제된 결제 내역은 주문 ID로 조회되지 않아야 한다")
//    void softDeleteFilteringTest() {
//        // given: 결제 데이터 생성 후 삭제 처리
//        Payment payment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(15000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.READY)
//                .build();
//        paymentRepository.saveAndFlush(payment);
//
//        // Soft Delete 수행 (BaseEntity의 deletedAt 필드 업데이트)
//        org.springframework.test.util.ReflectionTestUtils.setField(payment, "deletedAt", java.time.LocalDateTime.now());
//        paymentRepository.saveAndFlush(payment);
//        entityManager.clear();
//
//        // when: 삭제된 데이터의 orderId로 조회
//        Optional<Payment> found = paymentRepository.findByOrderIdAndDeletedAtIsNull(sampleOrderId);
//
//        // then: 조회 결과가 없어야 함
//        assertThat(found).isEmpty();
//    }
//    @Test
//    @DisplayName("결제 존재 여부 확인: 동일한 주문 ID의 결제가 존재하는지 정확히 판별해야 한다")
//    void existsByOrderIdTest() {
//        // given
//        Payment payment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(20000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.READY)
//                .build();
//        paymentRepository.saveAndFlush(payment);
//
//        // when & then
//        // 1. 존재하는 주문 ID로 확인
//        boolean exists = paymentRepository.existsByOrderIdAndDeletedAtIsNull(sampleOrderId);
//        assertThat(exists).isTrue();
//
//        // 2. 존재하지 않는 다른 주문 ID로 확인
//        boolean notExists = paymentRepository.existsByOrderIdAndDeletedAtIsNull(UUID.randomUUID());
//        assertThat(notExists).isFalse();
//    }
//    @Test
//    @DisplayName("완료된 결제 조회: 여러 결제 시도 중 'DONE' 상태인 것만 조회해야 한다")
//    void findDonePaymentByOrderIdTest() {
//        // given: 한 주문(sampleOrderId)에 대해 실패한 결제와 성공한 결제가 섞여 있는 상황
//        Payment failedPayment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(50000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.FAILED)
//                .build();
//
//        Payment donePayment = Payment.builder()
//                .orderId(sampleOrderId)
//                .amount(BigDecimal.valueOf(50000))
//                .method(PaymentMethod.CARD)
//                .status(PaymentStatus.DONE) // 🚩 우리가 찾는 상태
//                .pgPaymentKey("success_key_123")
//                .build();
//
//        paymentRepository.save(failedPayment);
//        paymentRepository.save(donePayment);
//        paymentRepository.saveAndFlush(donePayment);
//        entityManager.clear();
//
//        // when: DONE 상태 결제 조회
//        Optional<Payment> result = paymentRepository.findDonePaymentByOrderId(sampleOrderId);
//
//        // then
//        assertThat(result).isPresent();
//        assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.DONE);
//        assertThat(result.get().getPgPaymentKey()).isEqualTo("success_key_123");
//    }
//}
