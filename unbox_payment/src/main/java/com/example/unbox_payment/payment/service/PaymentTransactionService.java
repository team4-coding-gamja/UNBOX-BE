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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${payment.toss.seller-key:MOCK_SELLER_KEY_TEST}")
    private String pgSellerKey;

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

        // 결제 완료 처리
        payment.completePayment(response.getPaymentKey(), response.getApproveNo());

        // 판매 입찰 상태 변경 (RESERVED → SOLD)
        tradeClient.soldSellingBid(orderInfo.getSellingBidId(), "payment-service");

        // 주문 상태 변경 (PAYMENT_PENDING → PENDING_SHIPMENT)
        orderClient.pendingShipmentOrder(orderInfo.getOrderId(), "payment-service");

        // PG 트랜잭션 로그 저장 (성공)
        PgTransaction transaction = pgTransactionMapper.toSuccessEntity(payment, response, pgSellerKey);
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