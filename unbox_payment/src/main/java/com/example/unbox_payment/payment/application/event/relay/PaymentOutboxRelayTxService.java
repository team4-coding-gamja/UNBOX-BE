package com.example.unbox_payment.payment.application.event.relay;

import com.example.unbox_payment.payment.application.event.producer.PaymentEventProducer;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEventStatus;
import com.example.unbox_payment.payment.domain.repository.PaymentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 릴레이의 트랜잭션 경계를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxRelayTxService {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;

    /**
     * PENDING 이벤트를 SKIP LOCKED로 가져와 PROCESSING으로 선점한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimPendingEvents(int batchSize) {
        List<PaymentOutboxEvent> pendingEvents = paymentOutboxEventRepository
                .lockNextPending(PaymentOutboxEventStatus.PENDING.name(), batchSize);

        if (pendingEvents.isEmpty()) {
            return List.of();
        }

        pendingEvents.forEach(PaymentOutboxEvent::markAsProcessing);
        paymentOutboxEventRepository.saveAll(pendingEvents);

        List<UUID> eventIds = pendingEvents.stream()
                .map(PaymentOutboxEvent::getId)
                .toList();

        log.debug("[OutboxRelayTx] {}개 이벤트 PROCESSING 상태 변경 완료", eventIds.size());
        return eventIds;
    }

    /**
     * 이벤트 하나를 독립 트랜잭션으로 발행한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEventInSeparateTransaction(UUID eventId, int maxRetryCount) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        if (event.getStatus() != PaymentOutboxEventStatus.PROCESSING) {
            log.warn("[OutboxRelayTx] 이미 처리된 이벤트 - eventId: {}, status: {}", eventId, event.getStatus());
            return;
        }

        try {
            paymentEventProducer.publishEvent(event);
            log.debug("[OutboxRelayTx] 이벤트 발행 완료 - eventId: {}", eventId);
            return;
        } catch (Exception e) {
            log.error("[OutboxRelayTx] 이벤트 발행 실패 - eventId: {}, error: {}", eventId, e.getMessage(), e);
        }

        if (event.getRetryCount() >= maxRetryCount) {
            event.markAsFailed();
            log.error("[OutboxRelayTx] 최대 재시도 횟수 초과 - eventId: {}, retryCount: {}",
                    event.getId(), event.getRetryCount());
        } else {
            event.markAsRetryPending();
            log.warn("[OutboxRelayTx] 재시도 예정 - eventId: {}, retryCount: {}/{}",
                    event.getId(), event.getRetryCount(), maxRetryCount);
        }

        paymentOutboxEventRepository.save(event);
    }

    /**
     * 장시간 PROCESSING 상태인 이벤트를 PENDING으로 복구한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recoverStuckProcessingEvents(LocalDateTime threshold, int maxRetryCount) {
        List<PaymentOutboxEvent> stuckEvents = paymentOutboxEventRepository
                .findStuckEvents(PaymentOutboxEventStatus.PROCESSING.name(), threshold);

        if (stuckEvents.isEmpty()) {
            return 0;
        }

        log.warn("[OutboxRelayTx] 워커 장애로 멈춘 이벤트 {}개 발견 - 복구 시작", stuckEvents.size());

        for (PaymentOutboxEvent event : stuckEvents) {
            long stuckMinutes = Duration.between(event.getCreatedAt(), LocalDateTime.now()).toMinutes();
            if (event.getRetryCount() >= maxRetryCount) {
                event.markAsFailed("stuck processing exceeded max retry");
                log.error("[OutboxRelayTx] 복구 중 FAILED 전환 - eventId: {}, retryCount: {}, stuckMinutes: {}",
                        event.getId(), event.getRetryCount(), stuckMinutes);
            } else {
                event.markAsRetry("worker failure recovery");
                log.info("[OutboxRelayTx] 복구 완료 - eventId: {}, retryCount: {}/{}, stuckMinutes: {}",
                        event.getId(), event.getRetryCount(), maxRetryCount, stuckMinutes);
            }
        }

        paymentOutboxEventRepository.saveAll(stuckEvents);
        log.info("[OutboxRelayTx] {}개 이벤트 복구 완료", stuckEvents.size());
        return stuckEvents.size();
    }
}
