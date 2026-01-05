package com.example.unbox_be.domain.payment.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.entity.PgTransaction;
import com.example.unbox_be.domain.payment.entity.PgTransactionStatus;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.payment.repository.PgTransactionRepository;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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
    private final PaymentRepository paymentRepository;         // 추가
    private final PgTransactionRepository pgTransactionRepository; // 추가
    private final SellingBidService sellingBidService;
    private final OrderRepository orderRepository;
    @Value("${payment.toss.seller-key:MOCK_SELLER_KEY_TEST}")
    private String pgSellerKey;

    @Transactional
    public void processSuccessfulPayment(UUID paymentId, TossConfirmResponse response, String paymentKeyFromFront) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.DONE) {
            log.warn("중복 결제 승인 시도 차단 - paymentId: {}", paymentId);
            throw new CustomException(ErrorCode.PG_PROCESSED_ALREADY_EXISTS);
        }

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 결제 가격 맞는지 한번 더 확인
        if (!payment.getAmount().equals(response.getTotalAmount().intValue())) {
            throw new CustomException(ErrorCode.PRICE_MISMATCH);
        }

        payment.completePayment(response.getPaymentKey());
        // 2) 상품 상태 변경 (HOLD -> MATCHED)
        sellingBidService.updateSellingBidStatus(
                order.getSellingBidId(), // 이제 정상 작동합니다!
                SellingStatus.MATCHED,
                null,
                "SYSTEM"
        );

        // 3) PG 트랜잭션 로그 저장
        PgTransaction transaction = PgTransaction.builder()
                .payment(payment)
                .pgPaymentKey(paymentKeyFromFront)
                .eventStatus(PgTransactionStatus.DONE)
                .eventType("PAYMENT")
                .rawPayload(response.getRawJson()) // 전체 응답값 저장
                .eventAmount(response.getTotalAmount().intValue())
                .pgProvider(response.getMethod())
                .pgSellerKey(pgSellerKey)
                .build();
        pgTransactionRepository.save(transaction);
    }

    @Transactional
    public void processFailedPayment(UUID paymentId, TossConfirmResponse response) {
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 결제 상태를 FAILED로 변경
        payment.changeStatus(PaymentStatus.FAILED);

        // 3. PG 트랜잭션에 실패 기록 저장
        PgTransaction transaction = PgTransaction.builder()
                .payment(payment)
                .pgPaymentKey(response != null ? response.getPaymentKey() : null)
                .eventStatus(PgTransactionStatus.FAILED) // 실패 상태로 저장
                .eventType("PAYMENT")
                .rawPayload(response != null ? response.getRawJson() : "API Response is Null")
                .eventAmount(response != null && response.getTotalAmount() != null
                        ? response.getTotalAmount().intValue() : payment.getAmount())

                .build();

        pgTransactionRepository.save(transaction);
    }
}