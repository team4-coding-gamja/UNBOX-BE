package com.example.unbox_be.domain.order.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    // int -> BigDecimal 변경
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

    // 생성자 파라미터 타입 변경 (int -> BigDecimal)
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
        this.status = OrderStatus.PENDING_SHIPMENT;
    }

    // 상태 변경 검증 로직 추가
    public void updateStatus(OrderStatus newStatus) {
        // 이미 완료되거나 취소된 건은 변경 불가
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