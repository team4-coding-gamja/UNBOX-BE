package com.example.unbox_be.order.order.entity;

import com.example.unbox_common.entity.BaseEntity;
import com.example.unbox_be.user.user.entity.User;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_inspections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Inspection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inspection_id", columnDefinition = "uuid")
    private UUID id;

    // --- 연관 관계 ---

    /*
     * [MSA 전환 시 리팩토링 포인트]
     * 현재: Order 엔티티와 1:1 강결합 (@OneToOne)
     * 미래(MSA): Order 도메인이 분리될 경우 직접 참조 불가.
     * 수정 가이드:
     * 1. `UUID orderId` 필드로 변경 (ID 참조)
     * 2. 검수 로직 수행 시 필요한 주문 정보(상품명 등)는 FeignClient로 조회하거나,
     * 검수 생성 시점에 필요한 정보를 Inspection 엔티티에 스냅샷으로 저장.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /*
     * [MSA 전환 시 리팩토링 포인트]
     * 현재: User(Admin) 엔티티 직접 참조.
     * 미래(MSA): User 도메인 분리 시 참조 불가.
     * 수정 가이드:
     * 1. `Long inspectorId` 필드로 변경.
     * 2. 검수자 실명 등이 필요하면 별도 조회 혹은 스냅샷 저장.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    // --- 검수 정보 ---

    @Column(columnDefinition = "TEXT")
    private String reason; // 불합격 사유 (합격 시에도 코멘트 남길 수 있음)

    @Enumerated(EnumType.STRING)
    @Column(name = "inspect_status", length = 20, nullable = false)
    private InspectionStatus inspectStatus;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 생성자 레벨 Builder
    // - 생성 시점에는 검수 대기(PENDING) 상태로 고정하여 무결성 보장
    @Builder
    public Inspection(Order order, User inspector) {
        this.order = order;
        this.inspector = inspector;
        this.inspectStatus = InspectionStatus.PENDING;
    }

    // =================================================================
    // Business Logic
    // =================================================================

    /**
     * 검수 합격 처리
     */
    public void pass(String reason) {
        validatePendingStatus();
        this.inspectStatus = InspectionStatus.PASSED;
        this.reason = reason; // 합격 시에도 특이사항(박스 훼손 등) 기록 가능
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 검수 불합격 처리
     */
    public void fail(String reason) {
        validatePendingStatus();
        this.inspectStatus = InspectionStatus.FAILED;
        this.reason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 검수 상태 검증
     * - 이미 판정이 난 검수 건은 수정 불가
     */
    private void validatePendingStatus() {
        if (this.inspectStatus != InspectionStatus.PENDING) {
            // ErrorCode.INVALID_INSPECTION_STATUS 가 없다면 추가하거나, INVALID_ORDER_STATUS 등을 활용
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }
}