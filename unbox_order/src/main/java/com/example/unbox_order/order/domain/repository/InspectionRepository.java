package com.example.unbox_order.order.domain.repository;

import com.example.unbox_order.order.domain.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    // ✅ 단건 조회(삭제 제외)
    Optional<Inspection> findByIdAndDeletedAtIsNull(UUID id);

    // ✅ 주문 ID로 조회 (삭제 제외)
    Optional<Inspection> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    // ✅ 주문 검수 여부 확인
    boolean existsByOrderIdAndDeletedAtIsNull(UUID orderId);
}
