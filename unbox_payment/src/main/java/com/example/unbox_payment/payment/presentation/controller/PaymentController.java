package com.example.unbox_payment.payment.presentation.controller;

import com.example.unbox_payment.payment.presentation.controller.api.PaymentApi;
import com.example.unbox_payment.payment.presentation.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_payment.payment.presentation.dto.request.PaymentCreateRequestDto;
import com.example.unbox_payment.payment.presentation.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.presentation.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.presentation.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.application.service.PaymentService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    // ✅ 결제 이력 조회
    @Override
    @GetMapping("/history")
    public CustomApiResponse<List<PaymentHistoryResponseDto>> getPaymentHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PaymentHistoryResponseDto> response = paymentService.getPaymentHistory(userDetails.getUserId());
        return CustomApiResponse.success(response);
    }

    // ✅ 결제 준비 (초기 레코드 생성)
    @PostMapping("/ready")
    public CustomApiResponse<PaymentReadyResponseDto> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentCreateRequestDto request) {
        // 사용자 ID 추출
        Long userId = userDetails.getUserId();
        // 결제 준비 서비스 호출
        PaymentReadyResponseDto response = paymentService.createPayment(
                userId,
                request.orderId(),
                request.method());
        // 성공 응답 반환
        return CustomApiResponse.success(response);
    }

    // ✅ 결제 승인 처리
    @PostMapping("/confirm")
    public CustomApiResponse<TossConfirmResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequestDto request) {
        // 사용자 ID 추출
        Long userId = userDetails.getUserId();
        // 결제 승인 서비스 호출
        TossConfirmResponse response = paymentService.confirmPayment(
                userId,
                request.paymentId(),
                request.paymentKey(),
                request.amount());
        // 성공 응답 반환
        return CustomApiResponse.success(response);
    }

    // ✅ 환불 처리 (시나리오 2 테스트용)
    @PostMapping("/refund/{paymentId}")
    public CustomApiResponse<String> refundPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId) {
        
        // 환경 변수로 에러 시뮬레이션
        String testVersion = System.getenv("TEST_CANARY_DEPLOYMENT");
        log.info("Refund request - TEST_CANARY_DEPLOYMENT: {}", testVersion);
        
        if ("v2.1".equals(testVersion)) {
            // 10% 확률로 환불 실패 (카드사 타임아웃 시뮬레이션)
            if (Math.random() < 0.1) {
                log.error("Refund API timeout - Card Company X (Simulated Error)");
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Refund API timeout - Card Company X"
                );
            }
        }
        
        // 정상 환불 처리 (실제 로직은 생략)
        log.info("Refund processed successfully for paymentId: {}", paymentId);
        return CustomApiResponse.success("환불이 완료되었습니다.");
    }
}