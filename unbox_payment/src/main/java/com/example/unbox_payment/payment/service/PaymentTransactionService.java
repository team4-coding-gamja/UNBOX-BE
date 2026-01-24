package com.example.unbox_payment.payment.service;

import com.example.unbox_payment.common.client.order.OrderClient;
import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.entity.Payment;
import com.example.unbox_payment.payment.entity.PaymentStatus;
import com.example.unbox_payment.payment.entity.PgTransaction;
import com.example.unbox_payment.payment.mapper.PgTransactionMapper;
import com.example.unbox_payment.payment.repository.PaymentRepository;
import com.example.unbox_payment.payment.repository.PgTransactionRepository;
import com.example.unbox_payment.common.client.trade.TradeClient;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final PgTransactionRepository pgTransactionRepository;
    private final PgTransactionMapper pgTransactionMapper;
    private final TradeClient tradeClient;
    private final OrderClient orderClient;

    private static final Set<String> PAYABLE_STATUSES = Set.of("PAYMENT_PENDING");

    /**
     * ✅ 결제 승인 준비 (Transaction 1)
     * - 전파 레벨 REQUIRES_NEW를 사용하여 물리적으로 독립된 트랜잭션에서 실행
     * - 즉시 커밋되어 다른 스레드에서 IN_PROGRESS 상태를 볼 수 있게 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment prepareForConfirm(Long userId, UUID paymentId, BigDecimal amountFromFront) {
        log.info("[PaymentTransaction] 결제 승인 준비 시작 - paymentId: {}", paymentId);

        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 상태 검증
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
        if (payment.getStatus() == PaymentStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.PAYMENT_IN_PROGRESS);
        }
        if (payment.getStatus() != PaymentStatus.READY) {
            log.warn("[PaymentTransaction] 결제 가능한 상태가 아닙니다. - paymentId: {}, status: {}", paymentId,
                    payment.getStatus());
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 3. 타임아웃 검증 (10분)
        if (payment.isExpired()) {
            log.warn("[PaymentTransaction] 결제 유효 시간 만료 - paymentId: {}, readyAt: {}", paymentId, payment.getReadyAt());
            throw new CustomException(ErrorCode.PAYMENT_EXPIRED);
        }

        // 4. 금액 검증 (DB vs Front)
        if (payment.getAmount().compareTo(amountFromFront) != 0) {
            log.error("[PaymentTransaction] 금액 불일치! DB: {}, Front: {}", payment.getAmount(), amountFromFront);
            throw new CustomException(ErrorCode.AMOUNT_MISMATCH);
        }

        // 4. 주문 정보 조회 및 검증
        OrderForPaymentInfoResponse orderInfo = orderClient.getOrderForPayment(payment.getOrderId());
        if (orderInfo.getBuyerId() == null || !orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_SELF_ORDER_PAYMENT);
        }
        if (!PAYABLE_STATUSES.contains(orderInfo.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 5. 상태 변경 및 커밋 (낙관적 락 발동 지점)
        payment.changeStatus(PaymentStatus.IN_PROGRESS);
        paymentRepository.saveAndFlush(payment);

        log.info("[PaymentTransaction] 결제 준비 완료 (IN_PROGRESS) - paymentId: {}", paymentId);
        return payment;
    }

    // ✅ 결제 승인 성공 처리
    @Transactional
    public void processSuccessfulPayment(UUID paymentId, TossConfirmResponse response) {

        // 결제 정보 조회
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 중복 결제 승인 차단
        if (payment.getStatus() == PaymentStatus.DONE) {
            log.warn("중복 결제 승인 시도 차단 - paymentId: {}", paymentId);
            throw new CustomException(ErrorCode.PG_PROCESSED_ALREADY_EXISTS);
        }

        // 주문 정보 조회
        OrderForPaymentInfoResponse orderInfo = orderClient.getOrderForPayment(payment.getOrderId());

        // 결제 금액 검증
        if (payment.getAmount().compareTo(response.getTotalAmount()) != 0) {
            throw new CustomException(ErrorCode.PRICE_MISMATCH);
        }

        // 결제 완료 처리 (paymentKey만 전달)
        payment.completePayment(response.getPaymentKey());

        // 판매 입찰 상태 변경 (RESERVED → SOLD)
        tradeClient.soldSellingBid(orderInfo.getSellingBidId(), "payment-service");

        // 주문 상태 변경 (PAYMENT_PENDING → PENDING_SHIPMENT)
        orderClient.pendingShipmentOrder(orderInfo.getOrderId(), "payment-service");

        // PG 트랜잭션 로그 저장 (성공)
        PgTransaction transaction = pgTransactionMapper.toSuccessEntity(payment, response);
        pgTransactionRepository.save(transaction);
    }

    // ✅ 결제 승인 실패 처리
    @Transactional
    public void processFailedPayment(UUID paymentId, TossConfirmResponse response) {

        // 결제 정보 조회
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 주문 정보 조회
        OrderForPaymentInfoResponse orderInfo = orderClient.getOrderForPayment(payment.getOrderId());

        // 결제 상태를 FAILED로 변경
        payment.failPayment();

        // 판매 입찰 상태 복구 (RESERVED → LIVE)
        tradeClient.liveSellingBid(orderInfo.getSellingBidId(), "payment-service");

        // PG 트랜잭션 로그 저장 (실패)
        PgTransaction transaction = pgTransactionMapper.toFailedEntity(payment, response);
        pgTransactionRepository.save(transaction);
    }
}