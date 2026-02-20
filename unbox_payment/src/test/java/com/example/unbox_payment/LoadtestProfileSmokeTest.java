package com.example.unbox_payment;

import com.example.unbox_payment.mock.MockOrderController;
import com.example.unbox_payment.payment.application.event.relay.PaymentOutboxMessageRelay;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.profiles.active=loadtest",
                "spring.task.scheduling.enabled=false",
                "DB_DRIVER_CLASS_NAME=org.postgresql.Driver",
                "DB_URL=jdbc:postgresql://localhost:5432/unbox_payment_smoke",
                "DB_USERNAME=postgres",
                "DB_PASSWORD=postgres",
                "SPRING_JWT_SECRET=loadtest-smoke-jwt-secret-loadtest-smoke-jwt-secret",
                "payment.toss.secret-key=test_sk",
                "spring.datasource.hikari.initialization-fail-timeout=0",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
                "spring.autoconfigure.exclude="
                        + "org.redisson.spring.starter.RedissonAutoConfigurationV2,"
                        + "org.redisson.spring.starter.RedissonAutoConfigurationV4"
        })
class LoadtestProfileSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private PaymentOutboxMessageRelay paymentOutboxMessageRelay;

    @Test
    void loadtestProfileBootsWithSingleMockOrderController() {
        assertThat(applicationContext.containsBean("testPaymentController")).isTrue();
        assertThat(applicationContext.getBeanNamesForType(MockOrderController.class)).hasSize(1);
    }
}
