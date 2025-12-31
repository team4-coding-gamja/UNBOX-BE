package com.example.unbox_be.domain.order.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    // 상태 변경 메서드 (비즈니스 로직)
    public void updateStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 종료된 주문의 상태는 변경할 수 없습니다.");
        }
        this.status = newStatus;
    }

    public void registerTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void registerFinalTrackingNumber(String finalTrackingNumber) {
        this.finalTrackingNumber = finalTrackingNumber;
    }
}