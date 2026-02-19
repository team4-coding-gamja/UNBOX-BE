package com.example.unbox_payment.payment.application.service;

import com.example.unbox_payment.payment.domain.entity.PaymentOutboxEvent;
import com.example.unbox_payment.payment.domain.repository.PaymentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Outbox 이벤트 상태 관리 서비스
 * 
 * Self-Invocation 문제 해결을 위해 PaymentEventProducer와 물리적으로 분리
 * - PaymentEventProducer: Kafka 발행 (트랜잭션 없음, 네트워크 I/O)
 * - PaymentOutboxService: DB 상태 업데이트 (트랜잭션, 로컬 I/O)
 * 
 * 이렇게 분리하면:
 * 1. 스프링 AOP 프록시가 정상 작동 (@Transactional 적용됨)
 * 2. Kafka 전송 시간이 DB 커넥션을 점유하지 않음
 * 3. 관심사 분리(SoC)로 유지보수성 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

    /**
     * 이벤트 발행 성공 처리: PROCESSING → PUBLISHED
     * 
     * @Transactional이 프록시를 통해 호출되어 정상 작동함
     * 
     * @param eventId 발행 완료된 이벤트 ID
     */
    @Transactional
    public void markAsPublished(UUID eventId) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        event.markAsPublished();
        paymentOutboxEventRepository.save(event);

        log.debug("[OutboxService] 이벤트 상태를 PUBLISHED로 변경: eventId={}", eventId);
    }

    /**
     * 이벤트 발행 실패 처리: PROCESSING → PENDING (재시도 대기)
     * 
     * @param eventId 발행 실패한 이벤트 ID
     */
    @Transactional
    public void markAsRetryPending(UUID eventId) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        event.markAsRetryPending();
        paymentOutboxEventRepository.save(event);

        log.warn("[OutboxService] 이벤트를 재시도 대기 상태로 변경: eventId={}", eventId);
    }

    /**
     * 이벤트 발행 영구 실패 처리: PROCESSING → FAILED
     * 
     * @param eventId 영구 실패한 이벤트 ID
     */
    @Transactional
    public void markAsFailed(UUID eventId) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        event.markAsFailed();
        paymentOutboxEventRepository.save(event);

        log.error("[OutboxService] 이벤트를 영구 실패 상태로 변경: eventId={}", eventId);
    }

    /**
     * 이벤트를 처리 중 상태로 변경: PENDING → PROCESSING
     * 
     * @param eventId 처리 시작할 이벤트 ID
     */
    @Transactional
    public void markAsProcessing(UUID eventId) {
        PaymentOutboxEvent event = paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("OutboxEvent not found: " + eventId));

        event.markAsProcessing();
        paymentOutboxEventRepository.save(event);

        log.debug("[OutboxService] 이벤트 상태를 PROCESSING으로 변경: eventId={}", eventId);
    }
}
