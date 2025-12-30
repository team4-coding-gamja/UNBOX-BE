package com.example.unbox_be.domain.order.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_inspections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inspection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inspection_id", columnDefinition = "uuid")
    private UUID id;

    // 주문과 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 검수자 (Admin 대신 일단 User 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @Column(columnDefinition = "TEXT")
    private String reason; // 검수 사유

    @Enumerated(EnumType.STRING)
    @Column(name = "inspect_status", length = 20)
    private InspectionStatus inspectStatus;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Inspection(Order order, User inspector) {
        this.order = order;
        this.inspector = inspector;
        this.inspectStatus = InspectionStatus.PENDING;
    }

    public void pass(String reason) {
        this.inspectStatus = InspectionStatus.PASSED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.inspectStatus = InspectionStatus.FAILED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }
}