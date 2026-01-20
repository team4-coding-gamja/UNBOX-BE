package com.example.unbox_be.payment.service;

import com.example.unbox_be.order.order.entity.Order;
import com.example.unbox_be.order.order.repository.OrderRepository;
import com.example.unbox_be.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.payment.entity.Payment;
import com.example.unbox_be.payment.entity.PaymentStatus;
import com.example.unbox_be.payment.entity.PgTransaction;
import com.example.unbox_be.payment.mapper.PgTransactionMapper;
import com.example.unbox_be.payment.repository.PaymentRepository;
import com.example.unbox_be.payment.repository.PgTransactionRepository;
import com.example.unbox_be.trade.infrastructure.adapter.TradeClientAdapter;
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
        private final TradeClientAdapter tradeClientAdapter;
        private final OrderRepository orderRepository;
        private final PgTransactionMapper pgTransactionMapper;

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
                Order order = orderRepository.findByIdAndDeletedAtIsNull(payment.getOrderId())
                                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

                // 결제 금액 검증
                if (payment.getAmount().compareTo(response.getTotalAmount()) != 0) {
                        throw new CustomException(ErrorCode.PRICE_MISMATCH);
                }

                // 결제 완료 처리
                payment.completePayment(response.getPaymentKey(), response.getApproveNo());

                // 판매 입찰 상태 변경 (RESERVED → SOLD)
                tradeClientAdapter.updateSellingBidStatus(
                                order.getSellingBidId(),
                                "SOLD",
                                "SYSTEM");

                // 주문 상태 변경 (PAYMENT_PENDING → PENDING_SHIPMENT)
                order.updateStatusAfterPayment();

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
                Order order = orderRepository.findByIdAndDeletedAtIsNull(payment.getOrderId())
                                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

                // 결제 상태를 FAILED로 변경
                payment.failPayment();

                // 판매 입찰 상태 복구 (RESERVED → LIVE)
                tradeClientAdapter.updateSellingBidStatus(
                                order.getSellingBidId(),
                                "LIVE",
                                "SYSTEM");

                // PG 트랜잭션 로그 저장 (실패)
                PgTransaction transaction = pgTransactionMapper.toFailedEntity(payment, response);
                pgTransactionRepository.save(transaction);
        }
}