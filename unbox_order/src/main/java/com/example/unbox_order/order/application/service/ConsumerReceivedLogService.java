package com.example.unbox_order.order.application.service;

import com.example.unbox_common.event.EventEnvelope;
import com.example.unbox_common.event.payment.PaymentCompletedEvent;
import com.example.unbox_order.order.domain.entity.ConsumerReceivedLog;
import com.example.unbox_order.order.domain.repository.ConsumerReceivedLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumerReceivedLogService {

    private final ConsumerReceivedLogRepository consumerReceivedLogRepository;

    /**
     * 이벤트 수신 로그 기록 (중복 삽입 안전)
     *
     * 서비스 본 기능(주문 상태 변경)에 영향을 주지 않도록 내부에서 예외를 흡수합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentCompleted(EventEnvelope envelope, PaymentCompletedEvent event,
            String topic, String consumerGroup) {
        try {
            if (envelope == null || envelope.getEventId() == null || envelope.getAggregateId() == null || event == null
                    || event.paymentId() == null || event.paymentKey() == null || event.paymentKey().isBlank()) {
                log.warn("[ConsumerReceivedLogService] skip record due to missing required fields");
                return;
            }

            if (consumerReceivedLogRepository.existsByEventIdAndConsumerGroup(envelope.getEventId(), consumerGroup)) {
                return;
            }

            consumerReceivedLogRepository.save(
                    ConsumerReceivedLog.of(
                            envelope.getEventId(),
                            envelope.getEventType(),
                            envelope.getAggregateId(),
                            event.paymentId(),
                            event.paymentKey(),
                            topic,
                            consumerGroup));
        } catch (DataIntegrityViolationException e) {
            // 동시성/재시도로 인한 중복은 정상 흐름
            log.debug("[ConsumerReceivedLogService] duplicated event log ignored: eventId={}, group={}",
                    envelope != null ? envelope.getEventId() : null, consumerGroup);
        } catch (Exception e) {
            // 로그 적재 실패가 본 비즈니스 처리 실패로 전파되지 않도록 차단
            log.warn("[ConsumerReceivedLogService] failed to record consumer log: {}", e.getMessage());
        }
    }
}
