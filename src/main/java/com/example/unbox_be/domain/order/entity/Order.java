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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption productOption;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "receiver_address")
    private String receiverAddress;

    @Column(name = "receiver_zip_code", length = 50)
    private String receiverZipCode;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "final_tracking_number", length = 100)
    private String finalTrackingNumber;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // 생성자 레벨 @Builder
    // - ID, Audit 필드는 제외
    // - 필수 값만 파라미터로 받음
    // - status 같은 초기값은 내부에서 설정
    @Builder
    public Order(User buyer, User seller, ProductOption productOption, BigDecimal price,
                 String receiverName, String receiverPhone, String receiverAddress, String receiverZipCode) {
        this.buyer = buyer;
        this.seller = seller;
        this.productOption = productOption;
        this.price = price;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.receiverZipCode = receiverZipCode;
        this.status = OrderStatus.PENDING_SHIPMENT; // 초기 상태 강제
    }

    // 상태 변경 메서드
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 종료된 주문의 상태는 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void cancel() {
        // 이미 배송 중이거나 완료된 경우 예외 처리
        if (this.status == OrderStatus.SHIPPED_TO_CENTER || this.status == OrderStatus.SHIPPED_TO_BUYER || this.status == OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // 이미 취소된 경우도 체크
        if (this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public void registerTracking(String trackingNumber) {
        // 1. 사전 검증 (registerTracking만의 고유 로직)
        if (this.status != OrderStatus.PENDING_SHIPMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 2. 운송장 번호 저장
        this.trackingNumber = trackingNumber;

        // 3. 상태 변경은 updateStatus에게 위임! (재사용)
        updateStatus(OrderStatus.SHIPPED_TO_CENTER);
    }

    // 관리자/검수자용 상태 변경
    public void updateAdminStatus(OrderStatus newStatus, String finalTrackingNumber) {
        // 1. 구매자에게 발송(SHIPPED_TO_BUYER) 상태로 변경하려는데 운송장 번호가 없으면 에러
        if (newStatus == OrderStatus.SHIPPED_TO_BUYER) {
            if (finalTrackingNumber == null || finalTrackingNumber.isBlank()) {
                // "배송 시작 시 운송장 번호는 필수입니다" 라는 의미의 에러
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            this.finalTrackingNumber = finalTrackingNumber;
        }

        // 2. 검수 불합격(INSPECTION_FAILED) 시 추가 로직이 필요하다면 여기에 작성 (예: 환불 처리 로직 트리거 등)

        // 3. 공통 상태 변경 로직 재사용
        updateStatus(newStatus);
    }

    public void registerFinalTrackingNumber(String finalTrackingNumber) {
        this.finalTrackingNumber = finalTrackingNumber;
    }
}