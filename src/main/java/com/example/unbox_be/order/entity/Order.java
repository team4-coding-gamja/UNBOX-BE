package com.example.unbox_be.order.entity;

import com.example.unbox_be.common.entity.BaseEntity;
import com.example.unbox_be.user.user.entity.User;
import com.example.unbox_be.common.error.exception.CustomException;
import com.example.unbox_be.common.error.exception.ErrorCode;
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

    // ======================= ID 참조 =======================
    @Column(name = "selling_bid_id", nullable = false)
    private UUID sellingBidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "product_option_id", nullable = false)
    private UUID productOptionId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // ======================= 생성자 =======================
    @Builder
    public Order(UUID sellingBidId, User buyer, User seller, UUID productOptionId, UUID productId,
                 String productName, String modelNumber, String productOptionName, String productImageUrl, String brandName,
                 BigDecimal price, String receiverName, String receiverPhone, String receiverAddress, String receiverZipCode) {
        this.sellingBidId = sellingBidId;
        this.buyer = buyer;
        this.seller = seller;
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
        this.status = OrderStatus.PENDING_SHIPMENT; // 초기 상태 강제 설정
    }

    // ======================= 정적 메서드 =======================
    private void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = newStatus;
    }

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

    public void registerTracking(String trackingNumber) {
        if (this.status != OrderStatus.PENDING_SHIPMENT) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.trackingNumber = trackingNumber;
        updateStatus(OrderStatus.SHIPPED_TO_CENTER);
    }

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

    // ======================= 유효성 검사 메서드 =======================
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