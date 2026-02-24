package com.example.unbox_payment.test;

import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_payment.payment.presentation.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_payment.payment.presentation.dto.response.TossConfirmResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test/api/payment")
@RequiredArgsConstructor
public class TestPaymentController {

    private final TestPaymentService testPaymentService;
    
    @Value("${TEST_CANARY_DEPLOYMENT:v1}")
    private String deploymentVersion;

    // 버전 확인 엔드포인트 (Blue-Green 테스트용)
    @GetMapping("/version")
    public CustomApiResponse<Map<String, String>> getVersion() {
        return CustomApiResponse.success(Map.of(
            "version", deploymentVersion,
            "service", "payment-service",
            "strategy", "blue-green"
        ));
    }

    // 동기 테스트 진입점
    @PostMapping("/confirm/sync")
    public CustomApiResponse<TossConfirmResponse> confirmPaymentSync(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequestDto request,
            @RequestHeader(value = "X-Fault-Target", required = false) String faultTarget,
            @RequestHeader(value = "X-Fault-Delay-MS", required = false, defaultValue = "0") long faultDelayMs) {

        Long userId = userDetails != null ? userDetails.getUserId() : 1L;
        TossConfirmResponse response = testPaymentService.confirmPaymentSync(
                userId,
                request.paymentId(),
                request.paymentKey(),
                request.amount(),
                faultTarget,
                faultDelayMs);

        return CustomApiResponse.success(response);
    }

    // 비동기 테스트 진입점
    @PostMapping("/confirm/async")
    public CustomApiResponse<TossConfirmResponse> confirmPaymentAsync(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequestDto request) {

        Long userId = userDetails != null ? userDetails.getUserId() : 1L;
        TossConfirmResponse response = testPaymentService.confirmPaymentAsync(
                userId,
                request.paymentId(),
                request.paymentKey(),
                request.amount());

        return CustomApiResponse.success(response);
    }
}
