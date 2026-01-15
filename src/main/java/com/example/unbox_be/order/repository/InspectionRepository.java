package com.example.unbox_be.order.repository;

import com.example.unbox_be.order.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {

    // ✅ 단건 조회(삭제 제외)
    Optional<Inspection> findByIdAndDeletedAtIsNullAndDeletedAtIsNull(UUID id);
}
