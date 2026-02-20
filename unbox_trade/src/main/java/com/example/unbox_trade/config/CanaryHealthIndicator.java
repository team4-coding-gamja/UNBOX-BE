package com.example.unbox_trade.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CanaryHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Canary 배포 테스트 - 시나리오 2: Health Check 에러 시뮬레이션
        String testVersion = System.getenv("TEST_CANARY_DEPLOYMENT");
        
        if ("v2.1".equals(testVersion)) {
            // 30% 확률로 Health Check 실패
            if (Math.random() < 0.3) {
                return Health.down()
                        .withDetail("reason", "Canary Test - Health Check Failure")
                        .withDetail("version", "v2.1")
                        .build();
            }
        }
        
        return Health.up().build();
    }
}
