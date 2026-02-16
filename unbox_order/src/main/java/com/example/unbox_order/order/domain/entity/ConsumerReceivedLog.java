package com.example.unbox_order.order.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consumer_received_log", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consumer_received_event_group", columnNames = { "event_id", "consumer_group" })
}, indexes = {
        @Index(name = "idx_consumer_received_payment_key", columnList = "payment_key"),
        @Index(name = "idx_consumer_received_group_received", columnList = "consumer_group, received_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumerReceivedLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consumer_received_log_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "payment_key", nullable = false, length = 200)
    private String paymentKey;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Builder
    private ConsumerReceivedLog(UUID eventId, String eventType, UUID aggregateId, UUID paymentId,
            String paymentKey, String topic, String consumerGroup, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.paymentId = paymentId;
        this.paymentKey = paymentKey;
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.receivedAt = receivedAt;
    }

    public static ConsumerReceivedLog of(UUID eventId, String eventType, UUID aggregateId,
            UUID paymentId, String paymentKey, String topic, String consumerGroup) {
        return ConsumerReceivedLog.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .paymentId(paymentId)
                .paymentKey(paymentKey)
                .topic(topic)
                .consumerGroup(consumerGroup)
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
