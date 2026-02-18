package com.example.unbox_payment.payment.application.event.relay;

import com.example.unbox_payment.payment.application.event.producer.PaymentEventProducer;
import com.example.unbox_payment.payment.application.service.PaymentOutboxService;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEventStatus;
import com.example.unbox_payment.payment.domain.repository.PaymentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 아웃박스 메시지 릴레이 (Transactional Outbox Pattern with SKIP LOCKED)
 * 
 * === 아키텍처 의사결정 (Architecture Decision Record) ===
 * 
 * 문제: 다중 인스턴스 환경에서 동시성 제어 및 성능 최적화
 * 
 * 고려된 옵션:
 * 1. 일반 비관적 락: 락 대기(Blocking)로 인한 병목 발생
 * 2. 상태 선점(PROCESSING): 좀비 이벤트 처리를 위한 복잡한 Lease 관리 필요
 * 
 * 최종 결정: 비관적 락 + SKIP LOCKED + 상태 선점 하이브리드
 * - PostgreSQL의 SKIP LOCKED로 락 경합 시 대기 없이 다음 Row 조회
 * - 짧은 트랜잭션으로 상태만 PROCESSING으로 변경 후 즉시 커밋 (락 해제)
 * - 각 이벤트를 독립 트랜잭션으로 처리하여 진짜 부분 실패 격리
 * - PROCESSING 상태는 Kafka 전송 중에만 유지 (락은 이미 해제됨)
 * 
 * === SKIP LOCKED 올바른 사용법 ===
 * 
 * ❌ 잘못된 방법 (락을 오래 잡음):
 * 
 * @Transactional
 *                void process() {
 *                List<Event> events = lockNextPending(); // 락 획득
 *                for (event : events) {
 *                kafka.send(event); // 100ms × 100개 = 10초 동안 락 유지! ❌
 *                }
 *                // 커밋 → 락 해제
 *                }
 * 
 *                ✅ 올바른 방법 (락을 짧게 잡음):
 *                1단계: 짧은 트랜잭션으로 상태만 변경 (1ms 이내)
 * @Transactional
 *                List<UUID> claimEvents() {
 *                List<Event> events = lockNextPending(); // 락 획득
 *                events.forEach(e -> e.markAsProcessing()); // 상태 변경
 *                save(events);
 *                return ids; // 커밋 → 락 즉시 해제! ✅
 *                }
 * 
 *                2단계: 각 이벤트를 독립 트랜잭션으로 처리
 *                for (id : ids) {
 *                processInSeparateTx(id); // 락 없이 처리, 실패해도 다른 이벤트 영향 없음
 *                }
 * 
 *                === 왜 이 방식이 더 나은가? ===
 * 
 *                1. 락 유지 시간 최소화 (10초 → 1ms)
 *                - 락은 상태 변경 시에만 (1ms 이내)
 *                - Kafka 전송은 락 없이 진행 (100ms × 100개)
 *                - 다른 워커가 다음 배치를 빠르게 가져갈 수 있음
 * 
 *                2. 진짜 부분 실패 격리
 *                - 각 이벤트가 독립 트랜잭션
 *                - DB 예외 발생해도 다른 이벤트 롤백 안 됨
 *                - UnexpectedRollbackException 리스크 없음
 * 
 *                3. 워커 장애 시 복구
 *                - PROCESSING 상태로 멈춘 이벤트는 별도 스케줄러로 복구
 *                - 또는 created_at 기준으로 오래된 PROCESSING은 PENDING으로 복원
 * 
 *                주기적으로 PENDING 상태의 이벤트를 조회하여 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxMessageRelay {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final PaymentOutboxService paymentOutboxService; // 상태 관리 서비스

    private static final int MAX_RETRY_COUNT = 5;
    private static final int BATCH_SIZE = 100;

    /**
     * 1초마다 PENDING 상태의 이벤트를 조회하여 Kafka로 발행
     * 
     * 2단계 처리 전략:
     * 1단계: 짧은 트랜잭션으로 PENDING → PROCESSING 상태 변경 (락 즉시 해제)
     * 2단계: 각 이벤트를 독립 트랜잭션으로 Kafka 발행 (락 없이 처리)
     */
    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        // 1단계: 짧은 트랜잭션으로 이벤트 소유권 확보 (SKIP LOCKED)
        List<UUID> claimedEventIds = claimPendingEvents();

        if (claimedEventIds.isEmpty()) {
            return;
        }

        log.info("[OutboxRelay] {}개 이벤트 소유권 확보 완료 (SKIP LOCKED)", claimedEventIds.size());

        // 2단계: 각 이벤트를 독립 트랜잭션으로 처리
        for (UUID eventId : claimedEventIds) {
            processEventInSeparateTransaction(eventId);
        }
    }

    /**
     * 1단계: PENDING 이벤트를 PROCESSING으로 변경 (짧은 트랜잭션)
     * 
     * 핵심:
     * - FOR UPDATE SKIP LOCKED로 락 획득
     * - PaymentOutboxService를 통해 상태 변경 (일관성!)
     * - 즉시 커밋하여 락 해제 (1ms 이내)
     * - Kafka 전송 시간 동안 락을 잡지 않음!
     * 
     * @return 소유권을 확보한 이벤트 ID 리스트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected List<UUID> claimPendingEvents() {
        // SKIP LOCKED로 가용 이벤트 조회 및 락 획득
        List<PaymentOutboxEvent> pendingEvents = paymentOutboxEventRepository
                .lockNextPending(PaymentOutboxEventStatus.PENDING.name(), BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return List.of();
        }

        // 상태 변경: PENDING → PROCESSING (PaymentOutboxService 사용!)
        List<UUID> eventIds = pendingEvents.stream()
                .map(PaymentOutboxEvent::getId)
                .toList();

        // 각 이벤트를 PaymentOutboxService를 통해 상태 변경
        eventIds.forEach(paymentOutboxService::markAsProcessing);

        // ID 반환 (트랜잭션 커밋 → 락 즉시 해제!)
        return eventIds;
    }

    /**
     * 2단계: 개별 이벤트를 독립 트랜잭션으로 처리
     * 
     * 핵심:
     * - 각 이벤트가 독립 트랜잭션 (진짜 부분 실패 격리)
     * - Kafka 전송 중 예외 발생해도 다른 이벤트 영향 없음
     * - DB 예외 발생해도 UnexpectedRollbackException 없음
     * 
     * @param eventId 처리할 이벤트 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processEventInSeparateTransaction(UUID eventId) {
        try {
            // 이벤트 재조회 (영속성 컨텍스트에서 관리)
            PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

            // PROCESSING 상태 검증 (다른 워커가 처리했을 가능성 방어)
            if (event.getStatus() != PaymentOutboxEventStatus.PROCESSING) {
                log.warn("[OutboxRelay] 이미 처리된 이벤트 - eventId: {}, status: {}", eventId, event.getStatus());
                return;
            }

            // Kafka 발행 (동기 방식, 브로커 ACK 대기)
            // 이 시점에는 락이 없음! 다른 워커는 다음 배치 처리 중
            paymentEventProducer.publishEvent(event);

            log.debug("[OutboxRelay] 이벤트 처리 완료 - eventId: {}", eventId);

        } catch (Exception e) {
            log.error("[OutboxRelay] 이벤트 발행 실패 - eventId: {}, error: {}", eventId, e.getMessage(), e);
            handlePublishFailure(eventId, e);
        }
    }

    /**
     * 발행 실패 처리 (독립 트랜잭션)
     * 
     * - 재시도 가능: PENDING 상태로 복원 (PaymentOutboxService 사용)
     * - 최대 재시도 초과: FAILED 상태로 변경 (PaymentOutboxService 사용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void handlePublishFailure(UUID eventId, Exception exception) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        if (event.getRetryCount() >= MAX_RETRY_COUNT) {
            // 최대 재시도 횟수 초과 → FAILED (PaymentOutboxService 사용!)
            paymentOutboxService.markAsFailed(eventId);
            log.error("[OutboxRelay] ⚠️ 최대 재시도 횟수 초과 - eventId: {}, retryCount: {}, 운영팀 확인 필요",
                    event.getId(), event.getRetryCount());

            // TODO: 운영팀 알림 (Slack, PagerDuty 등)

        } else {
            // 재시도 가능: PENDING 상태로 복원 (PaymentOutboxService 사용!)
            paymentOutboxService.markAsRetryPending(eventId);
            log.warn("[OutboxRelay] 재시도 예정 - eventId: {}, retryCount: {}/{}, nextRetry: 1초 후",
                    event.getId(), event.getRetryCount(), MAX_RETRY_COUNT);
        }
    }

    /**
     * 워커 장애로 멈춘 PROCESSING 이벤트 복구 스케줄러
     * 
     * 문제 상황:
     * - 워커가 PROCESSING 상태로 변경 후 Kafka 전송 중 비정상 종료
     * - 해당 이벤트는 PROCESSING 상태로 영구히 멈춤
     * 
     * 복구 전략:
     * - 5분 이상 PROCESSING 상태인 이벤트를 "멈춘 것"으로 간주
     * - PENDING 상태로 복원하여 다음 스케줄러에서 재처리
     * 
     * 실행 주기:
     * - 1분마다 실행 (fixedDelay = 60000ms)
     * - 초기 지연 1분 (initialDelay = 60000ms)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void recoverStuckProcessingEvents() {
        // 5분 이상 PROCESSING 상태인 이벤트 조회
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusMinutes(5);
        List<PaymentOutboxEvent> stuckEvents = paymentOutboxEventRepository
                .findStuckEvents(PaymentOutboxEventStatus.PROCESSING.name(), threshold);

        if (stuckEvents.isEmpty()) {
            return;
        }

        log.warn("[OutboxRelay] ⚠️ 워커 장애로 멈춘 이벤트 {}개 발견 - 복구 시작", stuckEvents.size());

        // PENDING 상태로 복원 (재시도 횟수 증가)
        stuckEvents.forEach(event -> {
            event.markAsRetry("워커 장애로 인한 자동 복구");
            log.info("[OutboxRelay] 이벤트 복구 - eventId: {}, retryCount: {}/{}, stuckDuration: {}분",
                    event.getId(),
                    event.getRetryCount(),
                    MAX_RETRY_COUNT,
                    java.time.Duration.between(event.getCreatedAt(), java.time.LocalDateTime.now()).toMinutes());
        });

        paymentOutboxEventRepository.saveAll(stuckEvents);
        log.info("[OutboxRelay] ✅ {}개 이벤트 복구 완료 (PROCESSING → PENDING)", stuckEvents.size());
    }
}
