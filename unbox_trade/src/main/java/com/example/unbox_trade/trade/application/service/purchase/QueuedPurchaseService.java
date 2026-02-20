package com.example.unbox_trade.trade.application.service.purchase;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_trade.trade.presentation.dto.response.PurchaseQueueStatusResponseDto;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueuedPurchaseService implements PurchaseService {

    private static final String PURCHASE_QUEUE_KEY = "purchase:queue";
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT = new DefaultRedisScript<>(
            "local len = redis.call('LLEN', KEYS[1]); " +
            "if len >= tonumber(ARGV[1]) then return -1 end; " +
            "redis.call('RPUSH', KEYS[1], ARGV[2]); " +
            "return len + 1;",
            Long.class
    );

    private final RedisTemplate<String, Object> redisTemplate;
    private final Tracer tracer;
    private final Propagator propagator;

    @Value("${purchase.queue.max-size:1000}")
    private int maxQueueSize;

    @Override
    public void purchase(UUID sellingBidId, Long buyerId) {
        enqueue(sellingBidId, buyerId);
    }

    public PurchaseQueueStatusResponseDto enqueue(UUID sellingBidId, Long buyerId) {
        String traceContext = extractTraceContext();
        long enqueuedAt = System.currentTimeMillis();
        StringBuilder payloadBuilder = new StringBuilder()
                .append(sellingBidId)
                .append(":")
                .append(buyerId)
                .append(":")
                .append(enqueuedAt);
        if (traceContext != null && !traceContext.isBlank()) {
            payloadBuilder.append(":").append(traceContext);
        }
        String payload = payloadBuilder.toString();
        Long position = redisTemplate.execute(
                ENQUEUE_SCRIPT,
                List.of(PURCHASE_QUEUE_KEY),
                maxQueueSize,
                payload
        );

        if (position == null || position < 0) {
            throw new CustomException("대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요.", ErrorCode.SERVICE_UNAVAILABLE);
        }

        long aheadCount = position - 1;
        log.info("[Stage 6] Enqueued purchase request. bid={}, buyer={}, position={}, ahead={}",
                sellingBidId, buyerId, position, aheadCount);

        return new PurchaseQueueStatusResponseDto(position, aheadCount);
    }

    private String extractTraceContext() {
        Span current = tracer.currentSpan();
        if (current == null || current.isNoop()) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(current.context(), carrier, Map::put);
        if (carrier.isEmpty()) {
            return null;
        }
        return carrier.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("|"));
    }
}
