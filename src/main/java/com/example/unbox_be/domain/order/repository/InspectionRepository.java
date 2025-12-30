package com.example.unbox_be.domain.order.repository;

import com.example.unbox_be.domain.order.entity.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<Inspection, UUID> {
}