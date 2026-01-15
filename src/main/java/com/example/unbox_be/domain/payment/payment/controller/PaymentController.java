package com.example.unbox_be.domain.payment.payment.controller;

import com.example.unbox_be.domain.payment.payment.controller.api.PaymentApi;
import com.example.unbox_be.domain.payment.payment.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_be.domain.payment.payment.dto.request.PaymentCreateRequestDto;
import com.example.unbox_be.domain.payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.payment.service.PaymentService;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    /**
     * 1. 결제 초기 레코드 생성
     * 주문서 페이지에서 결제하기 버튼을 누를 때 호출됩니다.
     */
    @Override
    @PostMapping("/ready")
    public ResponseEntity<PaymentReadyResponseDto> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PaymentCreateRequestDto request
    ) {
        PaymentReadyResponseDto response = paymentService.createPayment(
                userDetails.getUserId(),
                request.orderId(),
                request.method()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. 결제 승인 처리 (Mock)
     * 가짜 결제 성공 후 이 API를 호출하면 p_payment와 p_pg_transaction이 업데이트됩니다.
     */
    @Override
    @PostMapping("/confirm")
    public ResponseEntity<TossConfirmResponse> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PaymentConfirmRequestDto request
    ) {
        TossConfirmResponse response = paymentService.confirmPayment(
                userDetails.getUserId(),
                request.paymentId(),
                request.paymentKey()
        );
        return ResponseEntity.ok(response);
    }
}