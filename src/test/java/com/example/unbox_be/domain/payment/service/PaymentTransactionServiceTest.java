package com.example.unbox_be.domain.payment.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.entity.PgTransaction;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.payment.repository.PgTransactionRepository;
import com.example.unbox_be.domain.payment.service.PaymentTransactionService;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class PaymentTransactionServiceTest {

    @InjectMocks
    private PaymentTransactionService paymentTransactionService;

    @Mock private PaymentRepository paymentRepository;
    @Mock private PgTransactionRepository pgTransactionRepository;
    @Mock private SellingBidService sellingBidService;
    @Mock private OrderRepository orderRepository;

    // 🚩 클래스 레벨에 공통 변수 선언 (이게 있어야 아래 메서드들이 인식함)
    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID sellingBidId = UUID.randomUUID();
    private final String paymentKey = "toss_payment_key_123";
    private final String pgSellerKey = "MOCK_SELLER_KEY_TEST";

    // 리플렉션 헬퍼 메서드
    private <T> T createMockEntity(Class<T> clazz, Object id) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T entity = constructor.newInstance();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    // @Value 필드 수동 주입 (Test 환경용)
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentTransactionService, "pgSellerKey", pgSellerKey);
    }

    @Nested
    @DisplayName("processSuccessfulPayment 테스트")
    class ProcessSuccessfulPaymentTest {

        private Payment createReadyPayment(int amount) throws Exception {
            Payment payment = createMockEntity(Payment.class, paymentId);
            ReflectionTestUtils.setField(payment, "orderId", orderId);
            ReflectionTestUtils.setField(payment, "amount", amount);
            ReflectionTestUtils.setField(payment, "status", PaymentStatus.READY);
            return payment;
        }

        private Order createMockOrder() throws Exception {
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "sellingBidId", sellingBidId);
            return order;
        }

        @Test
        @DisplayName("1. 성공 - 모든 상태 변경 및 트랜잭션 저장")
        void success_AllProcess() throws Exception {
            // given
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);
            given(response.getPaymentKey()).willReturn(paymentKey);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            // when
            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
            verify(sellingBidService).updateSellingBidStatusBySystem(eq(sellingBidId), eq(SellingStatus.MATCHED), anyString());
            verify(pgTransactionRepository).save(any(PgTransaction.class));
        }

        @Test
        @DisplayName("2. 실패 - 결제 정보 없음")
        void fail_PaymentNotFound() {
            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, null, paymentKey))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("3. 실패 - 이미 완료된 결제 (멱등성)")
        void fail_AlreadyDone() throws Exception {
            Payment payment = createReadyPayment(10000);
            ReflectionTestUtils.setField(payment, "status", PaymentStatus.DONE);
            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, null, paymentKey))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PG_PROCESSED_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("4. 실패 - 연결된 주문 정보 없음")
        void fail_OrderNotFound() throws Exception {
            Payment payment = createReadyPayment(10000);
            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, mock(TossConfirmResponse.class), paymentKey))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("5. 실패 - 금액 불일치")
        void fail_PriceMismatch() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(5000L); // 만원인데 5천원 응답

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRICE_MISMATCH);
        }

        @Test
        @DisplayName("6. 성공 - 카드 결제 기록 확인")
        void success_CardMethod() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);
            given(response.getMethod()).willReturn("CARD");

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(pgTransactionRepository).save(argThat(t -> t.getPgProvider().equals("CARD")));
        }

        @Test
        @DisplayName("7. 성공 - 계좌이체 결제 기록 확인")
        void success_TransferMethod() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);
            given(response.getMethod()).willReturn("TRANSFER");

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(pgTransactionRepository).save(argThat(t -> t.getPgProvider().equals("TRANSFER")));
        }

        @Test
        @DisplayName("8. 실패 - 상품 상태 변경 중 예외 발생 시 롤백 확인")
        void fail_SellingBidServiceError() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            doThrow(new RuntimeException("Service Error")).when(sellingBidService)
                    .updateSellingBidStatusBySystem(any(), any(), any());

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("9. 성공 - PG 셀러 키 저장 확인")
        void success_PgSellerKeySave() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(pgTransactionRepository).save(argThat(t -> t.getPgSellerKey().equals("MOCK_SELLER_KEY_TEST")));
        }

        @Test
        @DisplayName("10. 실패 - Toss 응답이 null일 때 NPE 방지")
        void fail_NullResponse() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order)); // 🚩 Order 반환 설정 추가

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, null, paymentKey))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("11. 성공 - 큰 금액 변환 확인")
        void success_LargeAmount() throws Exception {
            Payment payment = createReadyPayment(2000000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(2000000L);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(pgTransactionRepository).save(argThat(t -> t.getEventAmount() == 2000000));
        }

        @Test
        @DisplayName("12. 성공 - rawJson 저장 확인")
        void success_RawJsonSave() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);
            given(response.getRawJson()).willReturn("{\"test\":\"json\"}");

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(pgTransactionRepository).save(argThat(t -> t.getRawPayload().contains("json")));
        }

        @Test
        @DisplayName("13. 실패 - sellingBidId가 누락된 주문")
        void fail_SellingBidIdNull() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "sellingBidId", null);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);

            paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey);

            verify(sellingBidService).updateSellingBidStatusBySystem(isNull(), any(), any());
        }

        @Test
        @DisplayName("14. 실패 - 트랜잭션 로그 저장 DB 에러")
        void fail_LogSaveDbError() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
            given(pgTransactionRepository.save(any())).willThrow(new RuntimeException("DB Error"));

            assertThatThrownBy(() -> paymentTransactionService.processSuccessfulPayment(paymentId, response, paymentKey))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("15. 성공 - 결제 키 기록 일치 확인")
        void success_PaymentKeyMatch() throws Exception {
            Payment payment = createReadyPayment(10000);
            Order order = createMockOrder();
            TossConfirmResponse response = mock(TossConfirmResponse.class);
            given(response.getTotalAmount()).willReturn(10000L);

            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));

            paymentTransactionService.processSuccessfulPayment(paymentId, response, "FRONT_KEY_TEST");

            verify(pgTransactionRepository).save(argThat(t -> t.getPgPaymentKey().equals("FRONT_KEY_TEST")));
        }
    }
}