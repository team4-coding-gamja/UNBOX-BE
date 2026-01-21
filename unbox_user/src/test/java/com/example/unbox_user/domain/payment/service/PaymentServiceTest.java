//package com.example.unbox_be.domain.payment.service;
//
//import com.example.unbox_be.domain.order.entity.Order;
//import com.example.unbox_be.domain.order.entity.OrderStatus;
//import com.example.unbox_be.domain.order.repository.OrderRepository;
//import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
//import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
//import com.example.unbox_be.domain.payment.entity.Payment;
//import com.example.unbox_be.domain.payment.entity.PaymentMethod;
//import com.example.unbox_be.domain.payment.entity.PaymentStatus;
//import com.example.unbox_be.domain.payment.repository.PaymentRepository;
//import com.example.unbox_be.domain.settlement.service.SettlementService;
//import com.example.unbox_be.domain.user.entity.User;
//import com.example.unbox_be.global.error.exception.CustomException;
//import com.example.unbox_be.global.error.exception.ErrorCode;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.BDDMockito.given;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.assertj.core.api.Assertions.*;
//
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.lang.reflect.Constructor;
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.UUID;
//
//@ExtendWith(MockitoExtension.class)
//public class PaymentServiceTest {
//    @InjectMocks
//    private PaymentService paymentService;
//
//    @Mock private TossApiService tossApiService;
//    @Mock private PaymentRepository paymentRepository;
//    @Mock private OrderRepository orderRepository;
//    @Mock private PaymentTransactionService paymentTransactionService;
//    @Mock private SettlementService settlementService;
//    @Mock private Payment payment;
//    @Mock private Order order;
//
//    private <T> T createMockEntity(Class<T> clazz, Object id) throws Exception {
//        Constructor<T> constructor = clazz.getDeclaredConstructor();
//        constructor.setAccessible(true);
//        T entity = constructor.newInstance();
//        ReflectionTestUtils.setField(entity, "id", id);
//        return entity;
//    }
//
//    @Nested
//    @DisplayName("결제 생성(createPayment) 테스트")
//    class CreatePaymentTest {
//
//        @Test
//        @DisplayName("1. 실패 - 주문 조회가 안 될 때")
//        void createPayment_Fail_OrderNotFound() throws Exception {
//            // given
//            UUID orderId = UUID.randomUUID();
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(1L, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
//        }
//
//        @Test
//        @DisplayName("2. 실패 - 주문 구매자와 로그인 유저 불일치")
//        void createPayment_Fail_NotSelfOrder() throws Exception {
//            // given
//            Long loginUserId = 1L;
//            Long realBuyerId = 2L;
//            UUID orderId = UUID.randomUUID();
//
//            Order order = createMockEntity(Order.class, orderId);
//            User realBuyer = createMockEntity(User.class, realBuyerId);
//            ReflectionTestUtils.setField(order, "buyer", realBuyer);
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(loginUserId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_SELF_ORDER_PAYMENT);
//        }
//
//        @Test
//        @DisplayName("3. 실패 - 이미 DONE 상태인 결제가 존재함")
//        void createPayment_Fail_AlreadyDone() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(10000));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            Payment donePayment = createMockEntity(Payment.class, UUID.randomUUID());
//            ReflectionTestUtils.setField(donePayment, "status", PaymentStatus.DONE);
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(donePayment));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_EXISTS);
//        }
//
//        @Test
//        @DisplayName("4. 성공 - 이미 READY 상태인 결제가 있으면 기존 정보를 반환 (재사용)")
//        void createPayment_Success_ReuseReady() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            UUID existingPaymentId = UUID.randomUUID();
//
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(10000));
//
//            Payment readyPayment = createMockEntity(Payment.class, existingPaymentId);
//            ReflectionTestUtils.setField(readyPayment, "status", PaymentStatus.READY);
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(readyPayment));
//
//            // when
//            PaymentReadyResponseDto result = paymentService.createPayment(userId, orderId, PaymentMethod.CARD);
//
//            // then
//            assertThat(result.paymentId()).isEqualTo(existingPaymentId);
//            verify(paymentRepository, never()).save(any(Payment.class));
//        }
//
//        @Test
//        @DisplayName("5. 실패 - 주문 상태가 PENDING_SHIPMENT가 아닌 경우")
//        void createPayment_Fail_InvalidOrderStatus() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.INSPECTION_FAILED); // 잘못된 상태
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(10000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
//        }
//
//        @Test
//        @DisplayName("6. 성공 - 기존 결제가 없으면 새로운 결제를 생성하고 저장한다")
//        void createPayment_Success_NewPayment() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(15000));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT); // 본인의 성공 조건 상태
//
//            // 기존 결제 없음 설정
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
//
//            // save 호출 시 저장된 척 ID를 부여한 Payment 반환
//            UUID newPaymentId = UUID.randomUUID();
//            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> {
//                Payment saved = invocation.getArgument(0);
//                ReflectionTestUtils.setField(saved, "id", newPaymentId);
//                return saved;
//            });
//
//            // when
//            PaymentReadyResponseDto result = paymentService.createPayment(userId, orderId, PaymentMethod.CARD);
//
//            // then
//            assertThat(result.paymentId()).isEqualTo(newPaymentId);
//            verify(paymentRepository, times(1)).save(any(Payment.class)); // 이번엔 호출되어야 함!
//        }
//
//        @Test
//        @DisplayName("7. 실패 - 주문 금액이 0원 이하인 경우")
//        void createPayment_Fail_ZeroAmount() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            ReflectionTestUtils.setField(order, "price", BigDecimal.ZERO); // 0원 세팅
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BID_PRICE);
//        }
//
//        @Test
//        @DisplayName("8. 성공 - 이미 취소된(CANCELED) 결제가 존재할 때의 처리")
//        void createPayment_Check_ExistingCanceledPayment() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            Payment canceledPayment = createMockEntity(Payment.class, UUID.randomUUID());
//            ReflectionTestUtils.setField(canceledPayment, "status", PaymentStatus.CANCELED);
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(canceledPayment));
//
//            // 신규 저장 설정
//            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
//
//            // when
//            paymentService.createPayment(userId, orderId, PaymentMethod.CARD);
//
//            // then
//            verify(paymentRepository, times(1)).save(any(Payment.class)); // 취소된 게 있었으니 새로 저장해야 함
//        }
//
//        @Test
//        @DisplayName("9. 실패 - 취소된(CANCELED) 주문에 대해 결제 생성을 시도할 때")
//        void createPayment_Fail_CanceledOrder() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.CANCELLED); // 취소된 주문
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
//        }
//
//        @Test
//        @DisplayName("10. 실패 - 결제 수단이 null일 때")
//        void createPayment_Fail_InvalidMethodFormat() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, null))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_METHOD_INVALID);
//        }
//
//        @Test
//        @DisplayName("11. 실패 - DB 저장 중 예기치 못한 런타임 에러 발생")
//        void createPayment_Fail_DatabaseRuntimeError() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
//            given(paymentRepository.save(any(Payment.class))).willThrow(new RuntimeException("DB 접속 오류"));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(RuntimeException.class);
//        }
//
//        @Test
//        @DisplayName("12. 실패 - 데이터 무결성 오류: 주문에 구매자 정보가 없는 경우")
//        void createPayment_Fail_OrderBuyerIsNull() throws Exception {
//            // given
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", null); // 구매자 정보 누락
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(1L, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class); // 내부에서 NPE 방지 처리가 되어있어야 함
//        }
//
//        @Test
//        @DisplayName("13. 성공 - 매우 큰 금액(BigDecimal) 결제 생성 확인")
//        void createPayment_Success_LargeAmount() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            BigDecimal largePrice = new BigDecimal("999999999999.99");
//
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.PENDING_SHIPMENT);
//            ReflectionTestUtils.setField(order, "price", largePrice);
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//            given(paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)).willReturn(Optional.empty());
//            given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));
//
//            // when
//            PaymentReadyResponseDto result = paymentService.createPayment(userId, orderId, PaymentMethod.CARD);
//
//            // then
//            assertThat(result).isNotNull();
//            verify(paymentRepository).save(argThat(p -> p.getAmount().equals(largePrice)));
//        }
//        @Test
//        @DisplayName("14. 실패 - 이미 배송 중(SHIPPING)인 주문에 대해 결제 생성 시도")
//        void createPayment_Fail_AlreadyShipping() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.SHIPPED_TO_BUYER); // 이미 배송 중
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
//        }
//
//        @Test
//        @DisplayName("15. 실패 - 구매 확정(COMPLETED)된 주문에 대해 결제 생성 시도")
//        void createPayment_Fail_OrderCompleted() throws Exception {
//            // given
//            Long userId = 1L;
//            UUID orderId = UUID.randomUUID();
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", createMockEntity(User.class, userId));
//            ReflectionTestUtils.setField(order, "status", OrderStatus.COMPLETED);
//            ReflectionTestUtils.setField(order, "price", BigDecimal.valueOf(1000));
//
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            // when & then
//            assertThatThrownBy(() -> paymentService.createPayment(userId, orderId, PaymentMethod.CARD))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
//        }
//    }
//
//    @Nested
//    @DisplayName("결제 승인(confirmPayment) 테스트")
//    class ConfirmPaymentTest {
//
//        private final Long userId = 1L;
//        private final UUID paymentId = UUID.randomUUID();
//        private final UUID orderId = UUID.randomUUID();
//        private final String paymentKey = "toss_key_12345";
//        private final BigDecimal amount = BigDecimal.valueOf(10000);
//
//        // 매 테스트마다 반복되는 기본 세팅 (성공 케이스 기준)
//        private void setupBasicMocks(OrderStatus orderStatus, PaymentStatus paymentStatus) throws Exception {
//            User buyer = createMockEntity(User.class, userId);
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", buyer);
//            ReflectionTestUtils.setField(order, "status", orderStatus);
//            ReflectionTestUtils.setField(order, "price", amount);
//
//            Payment payment = createMockEntity(Payment.class, paymentId);
//            ReflectionTestUtils.setField(payment, "orderId", orderId);
//            ReflectionTestUtils.setField(payment, "amount", amount);
//            ReflectionTestUtils.setField(payment, "status", paymentStatus);
//
//            lenient().when(paymentRepository.findByIdAndDeletedAtIsNull(paymentId))
//                    .thenReturn(Optional.of(payment));
//
//            lenient().when(orderRepository.findByIdAndDeletedAtIsNull(orderId))
//                    .thenReturn(Optional.of(order));
//        }
//
//        @Test
//        @DisplayName("1. 성공 - 모든 조건이 완벽할 때 결제 승인 성공")
//        void confirmPayment_Success_AllFine() throws Exception {
//            // 1. Given
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//
//            TossConfirmResponse mockResponse = mock(TossConfirmResponse.class);
//            lenient().when(mockResponse.isSuccess()).thenReturn(true);
//
//            // TossApiService.confirm이 호출될 때 mockResponse를 반환하도록 확실히 고정
//            // any(BigDecimal.class)를 명시하거나, any()를 사용합니다.
//            given(tossApiService.confirm(anyString(), any(BigDecimal.class))).willReturn(mockResponse);
//
//            // 2. When
//            paymentService.confirmPayment(userId, paymentId, paymentKey);
//
//            // 3. Then
//            verify(paymentTransactionService).processSuccessfulPayment(eq(paymentId), any(TossConfirmResponse.class));
//            verify(settlementService).createSettlement(eq(paymentId), eq(orderId));
//        }
//
//        @Test
//        @DisplayName("2. 실패 - 존재하지 않는 결제 ID")
//        void confirmPayment_Fail_PaymentNotFound() {
//            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_NOT_FOUND);
//        }
//
//        @Test
//        @DisplayName("3. 실패 - 주문자와 승인 요청 유저 불일치")
//        void confirmPayment_Fail_UserMismatch() throws Exception {
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//            Long otherUserId = 999L; // 다른 유저
//
//            assertThatThrownBy(() -> paymentService.confirmPayment(otherUserId, paymentId, paymentKey))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_SELF_ORDER_PAYMENT);
//        }
//
//        @Test
//        @DisplayName("4. 실패 - 주문 상태가 PENDING_SHIPMENT가 아님")
//        void confirmPayment_Fail_InvalidOrderStatus() throws Exception {
//            setupBasicMocks(OrderStatus.COMPLETED, PaymentStatus.READY); // 이미 완료된 주문
//
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
//        }
//
//        @Test
//        @DisplayName("5. 실패 - Toss API 승인 실패 (잔액 부족 등)")
//        void confirmPayment_Fail_TossLogicError() throws Exception {
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//            TossConfirmResponse failResponse = mock(TossConfirmResponse.class);
//            given(failResponse.isSuccess()).willReturn(false); // 승인 실패
//            given(tossApiService.confirm(anyString(), any())).willReturn(failResponse);
//
//            assertThrows(CustomException.class, () -> {
//                paymentService.confirmPayment(userId, paymentId, paymentKey);
//            });
//
//            verify(paymentTransactionService).processFailedPayment(eq(paymentId), any());
//            verify(settlementService, never()).createSettlement(any(), any());
//        }
//
//        @Test
//        @DisplayName("6. 실패 - Toss API 호출 중 타임아웃/네트워크 에러")
//        void confirmPayment_Fail_TossNetworkError() throws Exception {
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//            given(tossApiService.confirm(anyString(), any())).willThrow(new RuntimeException("Network Timeout"));
//
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(RuntimeException.class);
//        }
//
//        @Test
//        @DisplayName("7. 실패 - 승인 성공 후 내부 로직 에러 시 Toss 자동 취소")
//        void confirmPayment_Fail_RollbackWithCancel() throws Exception {
//            // 1. 기초 Mock 설정 (상태값 세팅)
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//
//            // 2. 금액 설정 (BigDecimal로 통일)
//            BigDecimal amount = new BigDecimal("10000");
//            lenient().when(payment.getAmount()).thenReturn(amount);
//            lenient().when(order.getPrice()).thenReturn(amount);
//
//            // 3. Toss 응답 Mock 설정
//            TossConfirmResponse mockResponse = mock(TossConfirmResponse.class);
//            lenient().when(mockResponse.isSuccess()).thenReturn(true);
//            lenient().when(mockResponse.getPaymentKey()).thenReturn(paymentKey);
//            // Toss 응답 객체 내의 금액도 BigDecimal로 비교될 수 있으므로 설정 (타입에 따라 Long/BigDecimal 선택)
//            lenient().when(mockResponse.getTotalAmount()).thenReturn(BigDecimal.valueOf(10000));
//
//            // 4. Toss API 호출 결과 설정
//            given(tossApiService.confirm(anyString(), any())).willReturn(mockResponse);
//
//            // 5. 내부 로직 에러 강제 발생 (정산 생성 시 실패 유도)
//            doThrow(new RuntimeException("DB Error"))
//                    .when(settlementService).createSettlement(any(), any());
//
//            // 6. 실행 및 검증
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(RuntimeException.class);
//
//            // 7. 보상 트랜잭션(취소)이 호출되었는지 확인
//            verify(tossApiService).cancel(eq(paymentKey), anyString());
//        }
//
//        @Test
//        @DisplayName("8. 실패 - 주문 금액 정보가 누락된 데이터")
//        void confirmPayment_Fail_PriceNull() throws Exception {
//            User buyer = createMockEntity(User.class, userId);
//            Order order = createMockEntity(Order.class, orderId);
//            ReflectionTestUtils.setField(order, "buyer", buyer);
//            ReflectionTestUtils.setField(order, "price", null); // 금액 누락
//
//            Payment payment = createMockEntity(Payment.class, paymentId);
//            ReflectionTestUtils.setField(payment, "orderId", orderId);
//
//            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId)).willReturn(Optional.of(payment));
//            given(orderRepository.findByIdAndDeletedAtIsNull(orderId)).willReturn(Optional.of(order));
//
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_BID_PRICE);
//        }
//
//        @Test
//        @DisplayName("9. 실패 - 이미 완료된 결제건에 대한 중복 승인 시도 방지 (멱등성)")
//        void confirmPayment_Fail_AlreadyDonePayment() throws Exception {
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.DONE);
//
//            // when & then: 에러가 발생하는지 확인
//            assertThatThrownBy(() -> paymentService.confirmPayment(userId, paymentId, paymentKey))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_ALREADY_EXISTS);
//
//            // 승인 API가 호출되지 않아야 함
//            verify(tossApiService, never()).confirm(anyString(), any());
//        }
//
//        @Test
//        @DisplayName("10. 성공 - 결제 수단별(CARD) 승인 기록 확인")
//        void confirmPayment_Success_WithPaymentMethod() throws Exception {
//            setupBasicMocks(OrderStatus.PENDING_SHIPMENT, PaymentStatus.READY);
//            Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId).get();
//            ReflectionTestUtils.setField(payment, "method", PaymentMethod.CARD);
//
//            TossConfirmResponse mockResponse = mock(TossConfirmResponse.class);
//            lenient().when(mockResponse.isSuccess()).thenReturn(true);
//            lenient().when(mockResponse.getPaymentKey()).thenReturn(paymentKey);
//            given(tossApiService.confirm(anyString(), any())).willReturn(mockResponse);
//
//            paymentService.confirmPayment(userId, paymentId, paymentKey);
//
//            assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
//            verify(paymentTransactionService).processSuccessfulPayment(any(), any());
//        }
//
//        @Test
//        @DisplayName("낙관적 락 충돌 대응: DB 저장 시 버전 충돌이 발생하면 외부 결제를 취소해야 한다")
//        void confirmPayment_Fail_OptimisticLockConflict() throws Exception {
//            // 1. given: Mock이 아닌 "실제 객체" 생성 (본인 확인 통과용)
//            User mockBuyer = createMockEntity(User.class, 1L);
//            Order realOrder = createMockEntity(Order.class, orderId); // setupBasicMocks의 order 변수 대신 지역변수 사용 권장
//
//            ReflectionTestUtils.setField(realOrder, "buyer", mockBuyer);
//            ReflectionTestUtils.setField(realOrder, "price", BigDecimal.valueOf(10000));
//            ReflectionTestUtils.setField(realOrder, "status", OrderStatus.PENDING_SHIPMENT);
//
//            Payment realPayment = createMockEntity(Payment.class, paymentId);
//            ReflectionTestUtils.setField(realPayment, "orderId", orderId);
//            ReflectionTestUtils.setField(realPayment, "amount", BigDecimal.valueOf(10000));
//            ReflectionTestUtils.setField(realPayment, "status", PaymentStatus.READY);
//
//            // Stubbing: DB 조회 시 우리가 만든 실제 객체들을 반환하도록 설정
//            given(paymentRepository.findByIdAndDeletedAtIsNull(paymentId))
//                    .willReturn(Optional.of(realPayment));
//            given(orderRepository.findByIdAndDeletedAtIsNull(any()))
//                    .willReturn(Optional.of(realOrder));
//
//            // Toss API: 승인 성공 응답 세팅
//            TossConfirmResponse mockResponse = mock(TossConfirmResponse.class);
//            given(mockResponse.isSuccess()).willReturn(true);
//            given(tossApiService.confirm(anyString(), any())).willReturn(mockResponse);
//
//            // 🚩 낙관적 락 예외 강제 발생 설정
//            doThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(Payment.class, paymentId))
//                    .when(paymentTransactionService).processSuccessfulPayment(eq(paymentId), any());
//
//            // 2. when & then: 1L(userId)로 요청하여 본인 확인 통과 시키기
//            assertThatThrownBy(() -> paymentService.confirmPayment(1L, paymentId, paymentKey))
//                    .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
//
//            // 3. 검증: 락 충돌 시 cancel이 호출되었는가?
//            verify(tossApiService, times(1)).cancel(eq(paymentKey), anyString());
//        }
//    }
//}
