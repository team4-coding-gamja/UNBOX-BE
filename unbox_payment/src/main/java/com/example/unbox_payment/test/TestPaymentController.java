package com.example.unbox_payment.test;

import com.example.unbox_common.response.CustomApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/test/api/payment")
public class TestPaymentController {
    
    @Value("${TEST_CANARY_DEPLOYMENT:v1}")
    private String deploymentVersion;
    
    @Value("${TEST_ERROR_RATE:0}")
    private int errorRate; // 0-100 사이의 값, 에러 발생 확률
    
    private final Random random = new Random();

    // 버전 확인 엔드포인트 (Blue-Green 테스트용)
    @GetMapping("/version")
    public CustomApiResponse<Map<String, String>> getVersion() {
        // 50% 에러율 롤백 테스트
        if (errorRate > 0 && random.nextInt(100) < errorRate) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Simulated error for rollback test - version: " + deploymentVersion);
        }
        
        return CustomApiResponse.success(Map.of(
            "version", deploymentVersion,
            "service", "payment-service",
            "strategy", "blue-green",
            "scenario", "rollback-10percent-error",
            "errorRate", String.valueOf(errorRate) + "%"
        ));
    }
    
    // 헬스체크는 정상 응답 (Pod는 정상이지만 비즈니스 로직에서 에러 발생)
    @GetMapping("/health-test")
    public CustomApiResponse<Map<String, String>> healthTest() {
        return CustomApiResponse.success(Map.of(
            "status", "UP",
            "version", deploymentVersion
        ));
    }
}
