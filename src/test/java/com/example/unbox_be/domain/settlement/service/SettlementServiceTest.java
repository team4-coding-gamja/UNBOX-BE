package com.example.unbox_be.domain.settlement.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentMethod;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.settlement.dto.response.SettlementResponseDto;
import com.example.unbox_be.domain.settlement.entity.Settlement;
import com.example.unbox_be.domain.settlement.entity.SettlementStatus;
import com.example.unbox_be.domain.settlement.repository.SettlementRepository;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.BDDMockito.given;

import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private SellingBidRepository sellingBidRepository;

    private final UUID orderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();
    private final Long sellerId = 1L; // 엔티티 설계에 맞춰 Long으로 설정
    private final UUID bidId = UUID.randomUUID();

    /**
     * 리플렉션 유틸리티: 엔티티 생성 및 ID 주입
     */
    private <T> T createMockEntity(Class<T> clazz, Object id) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T entity = constructor.newInstance();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Nested
    @DisplayName("정산 생성(createSettlement) 테스트")
    class CreateSettlementTest {
        @Test
        @DisplayName("성공 - 모든 조건 충족 시 정상 정산 생성")
        void CreateSettlement_success_AllConditionsMet() throws Exception {
            // given
            User seller = createMockEntity(User.class, sellerId);
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "seller", seller);
            ReflectionTestUtils.setField(order, "sellingBidId", bidId);

            Payment payment = createMockEntity(Payment.class, paymentId);
            ReflectionTestUtils.setField(payment, "orderId", orderId);
            ReflectionTestUtils.setField(payment, "amount", 100000);

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", sellerId);

            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.of(order)).when(orderRepository).findById(orderId);
            doReturn(Optional.of(payment)).when(paymentRepository).findById(paymentId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findById(bidId);
            when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            SettlementResponseDto result = settlementService.createSettlement(paymentId, orderId);

            // then
            assertEquals(3000, result.getFeesAmount());
            assertEquals(97000, result.getSettlementAmount());
            assertEquals(SettlementStatus.WAITING, result.getSettlementStatus());
        }

        @Test
        @DisplayName("성공 - 수수료 반올림 검증 (1235원 * 3% = 37.05원 -> 37원)")
        void CreateSettlement_success_RoundingTest() throws Exception {
            // given
            setupBasicSuccessMocks(1235);
            when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            SettlementResponseDto result = settlementService.createSettlement(paymentId, orderId);

            // then
            assertEquals(37, result.getFeesAmount());
        }

        @Test
        @DisplayName("성공 - 결제 금액이 0원일 때 정산 금액 0원 확인")
        void CreateSettlement_success_ZeroAmount() throws Exception {
            setupBasicSuccessMocks(0);
            when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SettlementResponseDto result = settlementService.createSettlement(paymentId, orderId);

            assertEquals(0, result.getSettlementAmount());
        }


        @Test
        @DisplayName("실패 - 이미 정산이 존재하는 주문")
        void CreateSettlement_fail_AlreadyExists() {
            doReturn(true).when(settlementRepository).existsByOrderId(orderId);

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.SETTLEMENT_ALREADY_EXISTS, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 결제 정보와 주문 ID가 불일치 (데이터 오염 차단)")
        void CreateSettlement_fail_PaymentOrderIdMismatch() throws Exception {
            setupBasicSuccessMocks(100000);
            Payment payment = (Payment) paymentRepository.findById(paymentId).get();
            ReflectionTestUtils.setField(payment, "orderId", UUID.randomUUID());

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.PAYMENT_SETTLEMENT_MISMATCH, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 판매자와 입찰 소유자가 불일치 (보안 검증)")
        void CreateSettlement_fail_SellerUserMismatch() throws Exception {
            setupBasicSuccessMocks(100000);
            // 입찰 데이터의 소유자를 다른 사람(999L)으로 변경
            SellingBid bid = (SellingBid) sellingBidRepository.findById(bidId).get();
            ReflectionTestUtils.setField(bid, "userId", 999L);

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.SETTLEMENT_SELLER_MISMATCH, ex.getErrorCode());
        }


        @Test
        @DisplayName("실패 - 주문(Order)을 찾을 수 없음")
        void CreateSettlement_fail_OrderNotFound() {
            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.empty()).when(orderRepository).findById(orderId);

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 결제(Payment)를 찾을 수 없음")
        void CreateSettlement_fail_PaymentNotFound() throws Exception {
            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.of(createMockEntity(Order.class, orderId))).when(orderRepository).findById(orderId);
            doReturn(Optional.empty()).when(paymentRepository).findById(paymentId);

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.PAYMENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 입찰(SellingBid)을 찾을 수 없음")
        void CreateSettlement_fail_BidNotFound() throws Exception {
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "sellingBidId", bidId);

            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.of(order)).when(orderRepository).findById(orderId);
            doReturn(Optional.of(createMockEntity(Payment.class, paymentId))).when(paymentRepository).findById(paymentId);
            doReturn(Optional.empty()).when(sellingBidRepository).findById(bidId);

            CustomException ex = assertThrows(CustomException.class, () -> settlementService.createSettlement(paymentId, orderId));
            assertEquals(ErrorCode.BID_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 판매자(User) 정보가 엔티티에 누락된 경우 판매자 누락 확인")
        void CreateSettlement_fail_SellerInfoNull() throws Exception {
            // given
            // 1. 주문 생성 (판매자를 null로 설정)
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "seller", null);
            ReflectionTestUtils.setField(order, "sellingBidId", bidId);

            // 2. 결제 정보 생성 (조회 성공을 위해 필요)
            Payment payment = createMockEntity(Payment.class, paymentId);
            ReflectionTestUtils.setField(payment, "orderId", orderId);

            // 3. 입찰 정보 생성 (조회 성공을 위해 필요)
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", sellerId);

            // 4. 모든 사전 조회 로직을 성공으로 Mocking
            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.of(order)).when(orderRepository).findById(orderId);
            doReturn(Optional.of(payment)).when(paymentRepository).findById(paymentId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findById(bidId); // 이 부분이 핵심!

            CustomException ex = assertThrows(CustomException.class, () ->
                    settlementService.createSettlement(paymentId, orderId)
            );
            assertEquals(ErrorCode.SETTLEMENT_SELLER_MISMATCH, ex.getErrorCode());
        }

        /** 성공 시나리오용 공통 Mock 설정 헬퍼 */
        private void setupBasicSuccessMocks(Integer amount) throws Exception {
            User seller = createMockEntity(User.class, sellerId);
            Order order = createMockEntity(Order.class, orderId);
            ReflectionTestUtils.setField(order, "seller", seller);
            ReflectionTestUtils.setField(order, "sellingBidId", bidId);

            Payment payment = createMockEntity(Payment.class, paymentId);
            ReflectionTestUtils.setField(payment, "orderId", orderId);
            ReflectionTestUtils.setField(payment, "amount", amount);

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", sellerId);

            doReturn(false).when(settlementRepository).existsByOrderId(orderId);
            doReturn(Optional.of(order)).when(orderRepository).findById(orderId);
            doReturn(Optional.of(payment)).when(paymentRepository).findById(paymentId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findById(bidId);
        }
    }

    @Nested
    @DisplayName("정산 확정 테스트 (confirmSettlement)")
    class ConfirmSettlementTest {

        @Test
        @DisplayName("성공 - WAITING 상태의 정산을 DONE으로 변경하고 완료 시간을 기록한다")
        void ConfirmSettlement_success_ConfirmSettlement() throws Exception {
            // given
            // 빌더를 통해 WAITING 상태의 정산 객체 생성
            Settlement settlement = Settlement.builder()
                    .orderId(orderId)
                    .settlementStatus(SettlementStatus.WAITING)
                    .build();

            // 리플렉션으로 ID 주입
            ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());

            doReturn(Optional.of(settlement)).when(settlementRepository).findByOrderId(orderId);

            // when
            SettlementResponseDto result = settlementService.confirmSettlement(orderId);

            // then
            assertAll(
                    () -> assertEquals(SettlementStatus.DONE, result.getSettlementStatus(), "상태가 DONE으로 변경되어야 함"),
                    () -> assertNotNull(settlement.getCompletedAt(), "완료 시간(completedAt)이 기록되어야 함")
            );
        }

        @Test
        @DisplayName("실패 - 정산 데이터를 찾을 수 없을 때 (SETTLEMENT_NOT_FOUND)")
        void ConfirmSettlement_fail_SettlementNotFound() {
            // given
            doReturn(Optional.empty()).when(settlementRepository).findByOrderId(orderId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> settlementService.confirmSettlement(orderId));
            assertEquals(ErrorCode.SETTLEMENT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 이미 완료(DONE)된 정산을 다시 확정하려 할 때 (SETTLEMENT_ALREADY_DONE)")
        void ConfirmSettlement_fail_AlreadyDone() {
            // given
            Settlement alreadyDoneSettlement = Settlement.builder()
                    .settlementStatus(SettlementStatus.WAITING)
                    .build();

            // 이미 완료 상태로 변경
            alreadyDoneSettlement.updateStatus(SettlementStatus.DONE);

            doReturn(Optional.of(alreadyDoneSettlement)).when(settlementRepository).findByOrderId(orderId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> settlementService.confirmSettlement(orderId));
            assertEquals(ErrorCode.SETTLEMENT_ALREADY_DONE, ex.getErrorCode());
        }

        @Test
        @DisplayName("성공 - 확정 후에도 정산 금액 등 기존 정보가 변하지 않는지 확인")
        void ConfirmSettlement_success_MaintainData() throws Exception {
            // given
            Integer originalAmount = 97000;
            Settlement settlement = Settlement.builder()
                    .orderId(orderId)
                    .settlementAmount(originalAmount)
                    .settlementStatus(SettlementStatus.WAITING)
                    .build();

            doReturn(Optional.of(settlement)).when(settlementRepository).findByOrderId(orderId);

            // when
            SettlementResponseDto result = settlementService.confirmSettlement(orderId);

            // then
            assertEquals(originalAmount, result.getSettlementAmount(), "정산 금액은 유지되어야 함");
        }
        @Test
        @DisplayName("실패 - 취소(CANCELLED)된 정산은 확정(DONE) 상태로 변경될 수 없다")
        void ConfirmSettlement_fail_ConfirmCancelledSettlement() throws Exception {
            // given
            Settlement cancelledSettlement = Settlement.builder()
                    .orderId(orderId)
                    .settlementStatus(SettlementStatus.CANCELLED) // 취소 상태
                    .build();

            doReturn(Optional.of(cancelledSettlement)).when(settlementRepository).findByOrderId(orderId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> settlementService.confirmSettlement(orderId));
            assertEquals(ErrorCode.INVALID_SETTLEMENT_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("성공 - 정산 확정 시 완료 시간은 현재 시간과 일치해야 한다")
        void ConfirmSettlement_success_CompletedAtTimestamp() throws Exception {
            // given
            Settlement settlement = Settlement.builder()
                    .orderId(orderId)
                    .settlementStatus(SettlementStatus.WAITING)
                    .build();
            doReturn(Optional.of(settlement)).when(settlementRepository).findByOrderId(orderId);

            // when
            LocalDateTime now = LocalDateTime.now();
            settlementService.confirmSettlement(orderId);

            // then
            // 완료 시간이 메서드 호출 시점인 'now' 이후(또는 동일)인지 검증
            assertTrue(settlement.getCompletedAt().isAfter(now) || settlement.getCompletedAt().isEqual(now));
        }

        @Test
        @DisplayName("성공 - 정산 확정 후 반환되는 DTO에 판매자 및 금액 정보가 정확히 포함된다")
        void ConfirmSettlement_success_ConfirmResponseData() throws Exception {
            // given
            Settlement settlement = Settlement.builder()
                    .orderId(orderId)
                    .sellerId(sellerId)
                    .settlementAmount(97000)
                    .settlementStatus(SettlementStatus.WAITING)
                    .build();
            doReturn(Optional.of(settlement)).when(settlementRepository).findByOrderId(orderId);

            // when
            SettlementResponseDto response = settlementService.confirmSettlement(orderId);

            // then
            assertAll(
                    () -> assertEquals(sellerId, response.getSellerId()),
                    () -> assertEquals(97000, response.getSettlementAmount()),
                    () -> assertEquals(SettlementStatus.DONE, response.getSettlementStatus())
            );
        }
    }
}