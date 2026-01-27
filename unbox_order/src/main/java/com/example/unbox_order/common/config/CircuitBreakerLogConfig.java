package com.example.unbox_order.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Circuit Breaker 상태 변화 로깅 설정.
 * CLOSED ↔ OPEN ↔ HALF_OPEN 상태 전환 시 로그를 출력합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircuitBreakerLogConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerEventConsumers() {
        // 이미 등록된 CircuitBreaker에 이벤트 리스너 등록
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerEventConsumer);

        // 새로 생성되는 CircuitBreaker에도 이벤트 리스너 등록
        circuitBreakerRegistry.getEventPublisher()
            .onEntryAdded(event -> registerEventConsumer(event.getAddedEntry()));
    }

    private void registerEventConsumer(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(this::logStateTransition)
            .onError(event -> log.debug("[CircuitBreaker] {} - Error: {}",
                event.getCircuitBreakerName(), event.getThrowable().getMessage()))
            .onSuccess(event -> log.trace("[CircuitBreaker] {} - Success ({}ms)",
                event.getCircuitBreakerName(), event.getElapsedDuration().toMillis()))
            .onCallNotPermitted(event -> log.warn("[CircuitBreaker] {} - Call Not Permitted (Circuit OPEN)",
                event.getCircuitBreakerName()));
    }

    private void logStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String fromState = event.getStateTransition().getFromState().name();
        String toState = event.getStateTransition().getToState().name();

        if ("OPEN".equals(toState)) {
            log.error("🔴 [CircuitBreaker] {} 상태 변경: {} → {} (서킷 열림 - 요청 차단 중)",
                event.getCircuitBreakerName(), fromState, toState);
        } else if ("CLOSED".equals(toState)) {
            log.info("🟢 [CircuitBreaker] {} 상태 변경: {} → {} (서킷 닫힘 - 정상 동작)",
                event.getCircuitBreakerName(), fromState, toState);
        } else if ("HALF_OPEN".equals(toState)) {
            log.warn("🟡 [CircuitBreaker] {} 상태 변경: {} → {} (서킷 반열림 - 복구 시도 중)",
                event.getCircuitBreakerName(), fromState, toState);
        } else {
            log.warn("🔄 [CircuitBreaker] {} 상태 변경: {} → {}",
                event.getCircuitBreakerName(), fromState, toState);
        }
    }
}
