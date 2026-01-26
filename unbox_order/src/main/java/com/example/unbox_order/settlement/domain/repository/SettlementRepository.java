package com.example.unbox_order.settlement.domain.repository;

import com.example.unbox_order.settlement.domain.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    // "어떻게 가져올지" 우리가 정의하는 부분
    // 주문 ID로 정산 내역 하나를 가져온다.
    Optional<Settlement> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    Optional<Settlement> findByPaymentId(UUID paymentId);
}
