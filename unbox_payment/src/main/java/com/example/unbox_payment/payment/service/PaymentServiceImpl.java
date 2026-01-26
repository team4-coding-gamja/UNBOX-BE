package com.example.unbox_payment.payment.service;

import com.example.unbox_payment.common.client.order.OrderClient;
import com.example.unbox_payment.common.client.order.dto.OrderForPaymentInfoResponse;
import com.example.unbox_payment.payment.dto.internal.PaymentForSettlementResponse;
import com.example.unbox_payment.payment.dto.internal.PaymentStatusResponse;
import com.example.unbox_payment.common.client.settlement.SettlementClient;
import com.example.unbox_payment.payment.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.entity.Payment;
import com.example.unbox_payment.payment.entity.PaymentMethod;
import com.example.unbox_payment.payment.entity.PaymentStatus;
import com.example.unbox_payment.payment.mapper.PaymentClientMapper;
import com.example.unbox_payment.payment.mapper.PaymentMapper;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_payment.payment.producer.PaymentEventProducer;
import com.example.unbox_payment.payment.repository.PaymentRepository;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> PAYABLE_STATUSES = Set.of("PAYMENT_PENDING");

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final TossApiService tossApiService;
    private final PaymentMapper paymentMapper;
    private final PaymentClientMapper paymentClientMapper;
    private final OrderClient orderClient;
    private final SettlementClient settlementClient;
    private final PaymentEventProducer paymentEventProducer;

    // ✅ 결제 이력 조회
    @Override
    @Transactional(readOnly = true)
    public List<PaymentHistoryResponseDto> getPaymentHistory(Long userId) {
        return paymentRepository.findAllByBuyerIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(paymentMapper::toHistoryResponseDto)
                .collect(Collectors.toList());
    }

    // ✅ 결제 준비 (초기 레코드 생성)
    @Override
    @Transactional
    public PaymentReadyResponseDto createPayment(Long userId, UUID orderId, PaymentMethod method) {
        // 결제 수단 유효성 검증
        if (method == null) {
            throw new CustomException(ErrorCode.PAYMENT_METHOD_INVALID);
        }

        // 주문 정보 조회
        OrderForPaymentInfoResponse orderInfo = orderClient.getOrderForPayment(orderId);

        // 구매자 존재 여부 및 본인 확인
        if (orderInfo.getBuyerId() == null || !orderInfo.getBuyerId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_SELF_ORDER_PAYMENT);
        }

        // 주문 금액 검증
        if (orderInfo.getPrice() == null || orderInfo.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // 주문 상태 검증 (결제 가능한 상태인지)
        if (!PAYABLE_STATUSES.contains(orderInfo.getStatus())) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 기존 결제 내역 확인
        Optional<Payment> existingPayment = paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId);

        // 기존 결제가 존재하는 경우 처리
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();

            // 이미 완료된 결제가 있는 경우 예외 발생
            if (payment.getStatus() == PaymentStatus.DONE) {
                throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
            }

            // 준비 상태인 경우 기존 정보 반환
            if (payment.getStatus() == PaymentStatus.READY) {
                return paymentMapper.toReadyResponseDto(payment, orderInfo);
            }
        }

        // 새로운 결제 엔티티 생성
        Payment payment = paymentMapper.toEntity(orderInfo, method);

        // 결제 정보 저장
        Payment savedPayment = paymentRepository.save(payment);

        // 응답 DTO 생성 및 반환
        return paymentMapper.toReadyResponseDto(savedPayment, orderInfo);
    }

    // ✅ 결제 승인 처리 (결제 입력 완료 후)
    @Override
    public TossConfirmResponse confirmPayment(Long userId, UUID paymentId, String paymentKeyFromFront,
            BigDecimal amountFromFront) {
        log.info("[PaymentConfirm] 결제 승인 프로세스 시작 (트랜잭션 분리) - paymentId: {}, userId: {}", paymentId, userId);

        // 검증 및 상태 변경 - IN_PROGRES (물리적으로 분리된 트랜잭션에서 실행되어 즉시 커밋됨 (커넥션 점유 해제))
        Payment payment = paymentTransactionService.prepareForConfirm(userId, paymentId, amountFromFront);

        // PG 결제 키 준비
        String finalPaymentKey = (paymentKeyFromFront == null || paymentKeyFromFront.isBlank())
                ? "mock_key_" + UUID.randomUUID().toString().substring(0, 8)
                : paymentKeyFromFront;

        // ✅ 테스트용 강제 승인 로직 (Development Only)
        // paymentKey가 "test_success"로 시작하면 실제 PG 연동 없이 성공 처리
        if (finalPaymentKey.startsWith("test_success")) {
            log.info("[PaymentConfirm] 테스트용 강제 승인 처리 (Mock) - paymentId: {}", paymentId);

            TossConfirmResponse mockResponse = TossConfirmResponse.builder()
                    .paymentKey(finalPaymentKey)
                    .orderId(payment.getOrderId().toString())
                    .totalAmount(payment.getAmount())
                    .method("CARD") // 테스트용 고정값
                    .status("DONE")
                    .approvedAt(java.time.LocalDateTime.now().toString())
                    .build();

            // 성공 로직 수행
            paymentTransactionService.processSuccessfulPayment(paymentId, mockResponse);

            // 정산 정보 생성
            settlementClient.createSettlementForPayment(paymentId);

            // 결제 완료 이벤트 발행
            paymentEventProducer.publishPaymentCompleted(
                    PaymentCompletedEvent.of(finalPaymentKey, payment.getOrderId(), payment.getSellingBidId(), payment.getAmount())
            );

            log.info("[PaymentConfirm] 테스트 결제 프로세스 완료 - paymentId: {}", paymentId);
            return mockResponse;
        }

        // 외부 API 호출(이 구간에서 지연이 발생해도 DB Connection Pool을 점유하지 않음!)
        log.info("[PaymentConfirm] Toss API 호출 시도 (트랜잭션 없음) - paymentId: {}", paymentId);
        TossConfirmResponse response = tossApiService.confirm(finalPaymentKey, payment.getOrderId(),
                payment.getAmount(), paymentId.toString());

        if (response.isSuccess()) {
            log.info("[PaymentConfirm] Toss 승인 성공 - 후속 작업 진행 (트랜잭션 시작) - paymentId: {}", paymentId);
            try {
                // 성공 처리 (DONE 변경 등 분리된 트랜잭션에서 처리)
                paymentTransactionService.processSuccessfulPayment(paymentId, response);

                // 정산 정보 생성 (비동기 처리 등이 권장되지만 현재는 동기 유지)
                settlementClient.createSettlementForPayment(paymentId);
                
                // 🔄 결제 완료 이벤트 발행 (비동기 - Trade, Notification Service)
                // Trade Service: RESERVED -> SOLD 상태 변경
                // Order Service: PAYMENT_PENDING -> PENDING_SHIPMENT (현재는 동기 호출이 없지만 추후 전환 가능)
                paymentEventProducer.publishPaymentCompleted(
                        PaymentCompletedEvent.of(finalPaymentKey, payment.getOrderId(), payment.getSellingBidId(), payment.getAmount())
                );

                log.info("[PaymentConfirm] 전체 결제 프로세스 완료 - paymentId: {}", paymentId);
            } catch (Exception e) {
                log.error("[PaymentConfirm] 결제 성공 후 시스템 처리 중 오류 발생 - 자동 취소 시도 - paymentId: {}, error: {}", paymentId,
                        e.getMessage());
                // PG사에 결제 취소 요청
                tossApiService.cancel(finalPaymentKey, "서버 내부 오류로 인한 자동 취소", paymentId.toString());
                throw e;
            }
            return response;
        } else {
            log.error("[PaymentConfirm] Toss 승인 실패 - 실패 처리 진행 (트랜잭션 시작) - paymentId: {}, code: {}, message: {}",
                    paymentId, response.getErrorCode(), response.getErrorMessage());
            // 실패 처리 (상태 변경 등 분리된 트랜잭션에서 처리)
            paymentTransactionService.processFailedPayment(paymentId, response);
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    // ✅ 결제 조회 (정산용)
    @Override
    @Transactional(readOnly = true)
    public PaymentForSettlementResponse getPaymentForSettlement(UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return paymentClientMapper.toPaymentForSettlementResponse(payment);
    }

    // ✅ 결제 상태 조회
    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(UUID orderId) {
        return paymentRepository.findByOrderIdAndDeletedAtIsNull(orderId)
                .map(payment -> PaymentStatusResponse.builder()
                        .orderId(payment.getOrderId())
                        .status(payment.getStatus().name())
                        .build())
                .orElse(PaymentStatusResponse.builder()
                        .orderId(orderId)
                        .status("NOT_FOUND")
                        .build());
    }
}
