package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseQueueWorker {

    private static final String PURCHASE_QUEUE_KEY = "purchase:queue";
    private static final String PURCHASE_DLQ_KEY = "purchase:queue:dlq";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CachedDistributedPurchaseService cachedDistributedPurchaseService;
    private final Tracer tracer;
    private final Propagator propagator;

    private static final Propagator.Getter<Map<String, String>> MAP_GETTER = (carrier, key) ->
            carrier == null ? null : carrier.get(key);

    @PostConstruct
    public void start() {
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "purchase-queue-worker");
            t.setDaemon(true);
            return t;
        }).submit(this::loop);
    }

    private void loop() {
        while (true) {
            String payloadStr = null;
            try {
                Object payload = redisTemplate.opsForList()
                        .leftPop(PURCHASE_QUEUE_KEY, 1, TimeUnit.SECONDS);
                if (payload == null) {
                    continue;
                }

                payloadStr = payload.toString();
                processPayload(payloadStr);
            } catch (Exception e) {
                log.error("[Stage 6] Worker failed to process queue item", e);
                if (payloadStr != null) {
                    if (shouldSendToDlq(e)) {
                        // 최소한의 복구: 재시도 가치가 있는 실패는 DLQ로 이동
                        redisTemplate.opsForList().rightPush(PURCHASE_DLQ_KEY, payloadStr);
                    } else {
                        log.info("[Stage 6] Skipped DLQ for non-retriable error. payload={}", payloadStr);
                    }
                }
            }
        }
    }

    private void processPayload(String payloadStr) {
        String[] parts = payloadStr.split(":", 4);
        if (parts.length < 2) {
            log.warn("[Stage 6] Invalid payload in queue: {}", payloadStr);
            return;
        }

        UUID sellingBidId = UUID.fromString(parts[0]);
        Long buyerId = Long.parseLong(parts[1]);
        Long enqueuedAt = null;
        String traceCarrier = null;
        if (parts.length >= 3) {
            String third = parts[2];
            if (third.matches("\\d+")) {
                enqueuedAt = Long.parseLong(third);
                if (parts.length >= 4) {
                    traceCarrier = parts[3];
                }
            } else {
                // backward compatibility: payload without timestamp
                traceCarrier = third;
            }
        }

        Map<String, String> carrier = parseTraceCarrier(traceCarrier);
        Span span = startSpan(carrier, enqueuedAt, sellingBidId, buyerId);
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            if (enqueuedAt != null) {
                long waitMs = System.currentTimeMillis() - enqueuedAt;
                span.tag("queue.wait.ms", String.valueOf(waitMs));
                span.tag("queue.enqueued_at", String.valueOf(enqueuedAt));
            }
            cachedDistributedPurchaseService.purchase(sellingBidId, buyerId);
        } finally {
            span.end();
        }
    }

    private Span startSpan(Map<String, String> carrier, Long enqueuedAt, UUID sellingBidId, Long buyerId) {
        Span.Builder builder;
        if (carrier != null && !carrier.isEmpty()) {
            builder = propagator.extract(carrier, MAP_GETTER);
        } else {
            builder = tracer.spanBuilder().setNoParent();
        }

        builder.name("purchase.queue.process");
        if (enqueuedAt != null) {
            builder.startTimestamp(enqueuedAt, TimeUnit.MILLISECONDS);
        }

        Span span = builder.start();
        span.tag("queue.type", "purchase");
        span.tag("sellingBidId", sellingBidId.toString());
        span.tag("buyerId", String.valueOf(buyerId));
        return span;
    }

    private Map<String, String> parseTraceCarrier(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Map.of();
        }
        if (!encoded.contains("=")) {
            // fallback: raw traceparent only
            return Map.of("traceparent", encoded);
        }
        Map<String, String> carrier = new HashMap<>();
        String[] pairs = encoded.split("\\|");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0 || idx == pair.length() - 1) {
                continue;
            }
            carrier.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return carrier;
    }

    private boolean shouldSendToDlq(Exception e) {
        if (!(e instanceof CustomException)) {
            return true;
        }
        CustomException ce = (CustomException) e;
        HttpStatus status = ce.getErrorCode().getStatus();
        return status.is5xxServerError();
    }
}
