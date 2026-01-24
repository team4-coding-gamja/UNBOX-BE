package com.example.unbox_payment.payment.controller;

import com.example.unbox_payment.payment.controller.api.PaymentApi;
import com.example.unbox_payment.payment.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_payment.payment.dto.request.PaymentCreateRequestDto;
import com.example.unbox_payment.payment.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_payment.payment.service.PaymentService;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}