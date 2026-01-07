package com.example.unbox_be.domain.order.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "selling_bid_id", nullable = false)
    private UUID sellingBidId;

    // --- 연관 관계 ---

    /*
     * [MSA 전환 시 리팩토링 포인트]
     * 현재: JPA 객체 참조 (@ManyToOne) -> 다른 도메인(User)과 강한 결합(Hard Coupling) 발생
     * 미래(MSA): User 도메인이 분리되면 DB Join 불가능.
     * 수정 가이드:
     * 1. User 객체 대신 `Long buyerId` 필드로 변경 (ID 참조)
     * 2. 사용자 정보가 필요할 땐 UserClient(Feign)를 통해 조회하거나,
     * 주문 시점에 필요한 정보(이름/연락처 등)를 Order 테이블에 스냅샷으로 저장해야 함.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /*
     * [MSA 전환 시 리팩토링 포인트]
     * 현재: Product 도메인과 강한 결합.
     * 미래(MSA): Product 도메인 분리 시 참조 불가.
     * 수정 가이드:
     * 1. `UUID productOptionId` 필드로 변경.
     * 2. 주문 생성 시점의 상품 데이터(상품명, 옵션명, 가격 등)를 OrderItem 혹은 Order 내부에
     * 별도 컬럼(Snapshot)으로 반드시 저장해야 함. (상품 가격 변동 시 주문 이력 보호 목적)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption productOption;

    // --- 주문 정보 ---
    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    // --- 배송 정보 ---
    @Column(name = "receiver_name", length = 50, nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20, nullable = false)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress;

    @Column(name = "receiver_zip_code", length = 50, nullable = false)
    private String receiverZipCode;

    // --- 운송장 및 상태 추적 ---
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber; // 판매자 -> 센터

    @Column(name = "final_tracking_number", length = 100)
    private String finalTrackingNumber; // 센터 -> 구매자

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 생성자 레벨 Builder
    @Builder
    public Order(UUID sellingBidId, User buyer, User seller, ProductOption productOption, BigDecimal price,
                 String receiverName, String receiverPhone, String receiverAddress, String receiverZipCode) {
        this.sellingBidId = sellingBidId;
        this.buyer = buyer;
        this.seller = seller;
        this.productOption = productOption;
        this.price = price;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.receiverZipCode = receiverZipCode;
        this.status = OrderStatus.PENDING_SHIPMENT; // 초기 상태 강제 설정
    }

    public Order(User buyer, User seller, ProductOption option, UUID sellingBidId, BigDecimal price, OrderStatus orderStatus, String receiverName, String receiverPhone, String receiverAddress, String receiverZip) {
        super();
    }

    // =================================================================
    // Business Logic (도메인 로직 캡슐화)
    // =================================================================

    /**
     * 내부 상태 변경 메서드 (공통 사용)
     */
    private void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = newStatus;
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED_TO_CENTER
                || this.status == OrderStatus.SHIPPED_TO_BUYER
                || this.status == OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        if (this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 판매자 운송장 등록 (판매자 -> 센터)
     */
    public void registerTracking(String trackingNumber) {
        if (this.status != OrderStatus.PENDING_SHIPMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.trackingNumber = trackingNumber;
        updateStatus(OrderStatus.SHIPPED_TO_CENTER);
    }

    /**
     * 관리자/검수자용 상태 변경
     */
    public void updateAdminStatus(OrderStatus newStatus, String finalTrackingNumber) {
        validateAdminStatusTransition(this.status, newStatus);

        if (newStatus == OrderStatus.SHIPPED_TO_BUYER) {
            if (finalTrackingNumber == null || finalTrackingNumber.isBlank()) {
                throw new CustomException(ErrorCode.TRACKING_NUMBER_REQUIRED);
            }
            this.finalTrackingNumber = finalTrackingNumber;
        }

        updateStatus(newStatus);
    }

    /**
     * 구매 확정
     */
    public void confirm(User requestUser) {
        if (!this.buyer.getId().equals(requestUser.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (this.status != OrderStatus.DELIVERED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 상태 전환 유효성 검증 로직
     */
    private void validateAdminStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        boolean isValid = switch (currentStatus) {
            case SHIPPED_TO_CENTER -> newStatus == OrderStatus.ARRIVED_AT_CENTER;
            case ARRIVED_AT_CENTER -> newStatus == OrderStatus.IN_INSPECTION
                    || newStatus == OrderStatus.INSPECTION_PASSED
                    || newStatus == OrderStatus.INSPECTION_FAILED;
            case IN_INSPECTION -> newStatus == OrderStatus.INSPECTION_PASSED
                    || newStatus == OrderStatus.INSPECTION_FAILED;
            case INSPECTION_PASSED -> newStatus == OrderStatus.SHIPPED_TO_BUYER;
            case SHIPPED_TO_BUYER -> newStatus == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!isValid) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }
}