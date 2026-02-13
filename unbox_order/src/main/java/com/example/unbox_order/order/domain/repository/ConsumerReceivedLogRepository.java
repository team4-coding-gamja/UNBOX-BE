package com.example.unbox_order.order.domain.repository;

import com.example.unbox_order.order.domain.entity.ConsumerReceivedLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsumerReceivedLogRepository extends JpaRepository<ConsumerReceivedLog, UUID> {

    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
