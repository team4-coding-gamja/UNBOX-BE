package com.example.unbox_order.order.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
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

    // ======================= 강한 ID 참조 =======================
    @Column(name = "selling_bid_id", nullable = false)
    private UUID sellingBidId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    // ======================= 약한 ID 참조 =======================
    @Column(name = "product_option_id", nullable = false)
    private UUID productOptionId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // ======================= 주문 정보 =======================
    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    // ======================= 배송 정보 =======================
    @Column(name = "receiver_name", length = 50, nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20, nullable = false)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress;

    @Column(name = "receiver_zip_code", length = 50, nullable = false)
    private String receiverZipCode;

    // ======================= 운송장 및 상태 정보 =======================
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber; // 판매자 -> 센터

    @Column(name = "final_tracking_number", length = 100)
    private String finalTrackingNumber; // 센터 -> 구매자

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 결제 완료 시 저장되는 paymentId (환불 시 사용)
    @Column(name = "payment_id")
    private UUID paymentId;

    // ======================= 구매자 스냅샷 =======================
    // 구매자 닉네임 스냅샷 (리뷰 작성 시 사용)
    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    // ======================= 상품 스냅샷 =======================
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "model_number", nullable = false)
    private String modelNumber;

    @Column(name = "product_option_name", nullable = false)
    private String productOptionName;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    // ======================= 생성자 =======================
    @Builder
    public Order(UUID sellingBidId, Long buyerId, Long sellerId, String buyerName,
            UUID productOptionId, UUID productId,
            String productName, String modelNumber, String productOptionName, String productImageUrl, String brandName,
            BigDecimal price, String receiverName, String receiverPhone, String receiverAddress,
            String receiverZipCode) {
        this.sellingBidId = sellingBidId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.productOptionId = productOptionId;
        this.productId = productId;
        this.productName = productName;
        this.modelNumber = modelNumber;
        this.productOptionName = productOptionName;
        this.productImageUrl = productImageUrl;
        this.brandName = brandName;
        this.price = price;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.receiverZipCode = receiverZipCode;
        this.status = OrderStatus.PAYMENT_PENDING; // ✅ 초기 상태: 결제 대기
    }

    // ======================= 비즈니스 로직 메서드 =======================

    // ✅ 결제 완료 후 상태 변경 (PAYMENT_PENDING → PENDING_SHIPMENT)
    public void updateStatusAfterPayment(UUID paymentId) {
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.paymentId = paymentId;
        this.status = OrderStatus.PENDING_SHIPMENT;
    }

    // ✅ 주문 취소 (결제 전)
    public void cancel() {
        // 배송 중이거나 완료된 주문은 취소 불가
        if (this.status == OrderStatus.SHIPPED_TO_CENTER
                || this.status == OrderStatus.SHIPPED_TO_BUYER
                || this.status == OrderStatus.COMPLETED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // 이미 취소된 주문은 재취소 불가
        if (this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    // ✅ 환불 요청 (결제 후, 구매자만)
    // 배송 대기(PENDING_SHIPMENT) 또는 배송 완료(DELIVERED) 상태에서만 가능
    public OrderStatus requestRefund(Long requestUserId) {
        // 본인 확인
        if (!this.buyerId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 환불 가능 상태 확인 (배송 대기 또는 배송 완료)
        if (this.status != OrderStatus.PENDING_SHIPMENT 
                && this.status != OrderStatus.DELIVERED) {
            throw new CustomException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        // paymentId가 없으면 결제 완료 상태가 아님
        if (this.paymentId == null) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        
        return previousStatus; // 이전 상태 반환 (이벤트 발행용)
    }

    // ✅ 운송장 번호 등록 (판매자 → 센터)
    public void registerTracking(String trackingNumber) {
        // 배송 대기 상태에서만 운송장 등록 가능
        if (this.status != OrderStatus.PENDING_SHIPMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.trackingNumber = trackingNumber;
        updateStatus(OrderStatus.SHIPPED_TO_CENTER);
    }

    // ✅ 검수 시작
    public void startInspection() {
        if (this.status != OrderStatus.ARRIVED_AT_CENTER) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        updateStatus(OrderStatus.IN_INSPECTION);
    }

    // ✅ 검수 합격
    public void passedInspection() {
        if (this.status != OrderStatus.IN_INSPECTION) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        updateStatus(OrderStatus.INSPECTION_PASSED);
    }

    // ✅ 검수 불합격
    public void failedInspection() {
        if (this.status != OrderStatus.IN_INSPECTION) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        }
        updateStatus(OrderStatus.INSPECTION_FAILED);
    }

    // ✅ 관리자 상태 변경 (검수 프로세스)
    public void updateAdminStatus(OrderStatus newStatus, String finalTrackingNumber) {
        // 상태 전이 유효성 검증
        validateAdminStatusTransition(this.status, newStatus);

        // ... 나머지 로직 유지 ...
        if (newStatus == OrderStatus.SHIPPED_TO_BUYER) {
            if (finalTrackingNumber == null || finalTrackingNumber.isBlank()) {
                throw new CustomException(ErrorCode.TRACKING_NUMBER_REQUIRED);
            }
            this.finalTrackingNumber = finalTrackingNumber;
        }

        updateStatus(newStatus);
    }

    // ✅ 구매 확정
    public void confirm(Long requestUserId) {
        // 본인 확인
        if (!this.buyerId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 배송 완료 상태에서만 구매 확정 가능
        if (this.status != OrderStatus.DELIVERED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    // ✅ 상태 변경 (완료/취소된 주문은 변경 불가)
    private void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = newStatus;
    }

    // ✅ 관리자 상태 전이 유효성 검증
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