package com.example.unbox_payment.test;

import com.example.unbox_common.response.CustomApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/test/api/payment")
public class TestPaymentController {
    
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
}
