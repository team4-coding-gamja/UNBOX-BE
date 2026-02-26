package com.example.unbox_payment.payment.application.event.relay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbox 릴레이 스케줄러 진입점.
 * 실제 트랜잭션 경계(REQUIRES_NEW)는 PaymentOutboxRelayTxService에서 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxMessageRelay {

    private static final int MAX_RETRY_COUNT = 5;
    private static final int BATCH_SIZE = 100;
    private static final int STUCK_MINUTES = 5;

    private final PaymentOutboxRelayTxService relayTxService;

    /**
     * 1초마다 PENDING 이벤트를 클레임하고 발행한다.
     */
    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<UUID> claimedEventIds = relayTxService.claimPendingEvents(BATCH_SIZE);
        if (claimedEventIds.isEmpty()) {
            return;
        }

        log.info("[OutboxRelay] {}개 이벤트 소유권 확보 완료", claimedEventIds.size());
        for (UUID eventId : claimedEventIds) {
            relayTxService.processEventInSeparateTransaction(eventId, MAX_RETRY_COUNT);
        }
    }

    /**
     * 워커 장애 등으로 장시간 PROCESSING 상태에 머문 이벤트를 복구한다.
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void recoverStuckProcessingEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_MINUTES);
        relayTxService.recoverStuckProcessingEvents(threshold, MAX_RETRY_COUNT);
    }
}
