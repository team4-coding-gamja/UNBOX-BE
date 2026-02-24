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

    // 버전 확인 엔드포인트 (Blue-Green 정상 배포 시나리오 1)
    @GetMapping("/version")
    public CustomApiResponse<Map<String, String>> getVersion() {
        return CustomApiResponse.success(Map.of(
            "version", deploymentVersion,
            "service", "payment-service",
            "strategy", "blue-green",
            "scenario", "scenario-1-normal-deployment"
        ));
    }
}
