package com.example.unbox_be.domain.settlement.repository;

import com.example.unbox_be.domain.settlement.entity.Settlement;
import com.example.unbox_be.domain.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    // "어떻게 가져올지" 우리가 정의하는 부분
    // 주문 ID로 정산 내역 하나를 가져온다.
    Optional<Settlement> findByOrderId(UUID orderId);

    // 특정 판매자의 특정 상태인 정산 내역들을 모두 가져온다.
    List<Settlement> findAllBySellerIdAndSettlementStatus(Long sellerId, SettlementStatus status);

    boolean existsByOrderId(UUID orderId);
}
