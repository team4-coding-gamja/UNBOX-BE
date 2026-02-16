package com.example.unbox_payment.payment.test;

import com.example.unbox_payment.payment.domain.entity.PaymentMethod;
import com.example.unbox_payment.payment.domain.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPaymentDataGenerator {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void generateData(int count) {
        log.info("Starting data generation for {} records...", count);
        String sql = "INSERT INTO p_payment " +
                "(payment_id, order_id, buyer_id, seller_id, amount, status, method, created_at, updated_at, version) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            UUID paymentId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            long buyerId = ThreadLocalRandom.current().nextLong(1, 10000);
            long sellerId = ThreadLocalRandom.current().nextLong(1, 10000);
            BigDecimal amount = BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(1000, 1000000));

            // Distributed status: DONE 60%, READY 20%, others 20%
            PaymentStatus status = getRandomStatus();
            PaymentMethod method = PaymentMethod.values()[ThreadLocalRandom.current()
                    .nextInt(PaymentMethod.values().length)];

            // Random created_at within last 365 days
            LocalDateTime createdAt = now.minusDays(ThreadLocalRandom.current().nextLong(365));

            batchArgs.add(new Object[] {
                    paymentId,
                    orderId,
                    buyerId,
                    sellerId,
                    amount,
                    status.name(),
                    method.name(),
                    Timestamp.valueOf(createdAt),
                    Timestamp.valueOf(createdAt),
                    0L
            });

            if (batchArgs.size() >= 1000) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        log.info("Data generation completed.");
    }

    private PaymentStatus getRandomStatus() {
        int rand = ThreadLocalRandom.current().nextInt(100);
        if (rand < 60)
            return PaymentStatus.DONE;
        if (rand < 80)
            return PaymentStatus.READY;
        return PaymentStatus.FAILED; // Simplification
    }
}
