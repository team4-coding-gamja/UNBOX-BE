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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @Column(columnDefinition = "TEXT")
    private String reason;

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

    // 상태 검증 로직 추가
    public void pass(String reason) {
        if (this.inspectStatus != InspectionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 검수만 처리할 수 있습니다.");
        }
        this.inspectStatus = InspectionStatus.PASSED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }

    // 상태 검증 로직 추가
    public void fail(String reason) {
        if (this.inspectStatus != InspectionStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 검수만 처리할 수 있습니다.");
        }
        this.inspectStatus = InspectionStatus.FAILED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }
}