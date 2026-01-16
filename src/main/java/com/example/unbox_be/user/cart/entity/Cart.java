package com.example.unbox_be.user.cart.entity;

import com.example.unbox_common.entity.BaseEntity;
import com.example.unbox_be.user.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ======================= ID 참조 =======================
    @Column(name = "selling_bid_id", nullable = false)
    private UUID sellingBidId;

    // ======================= 상품 스냅샷 =======================
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_option_id", nullable = false)
    private UUID productOptionId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "model_number", nullable = false)
    private String modelNumber;

    @Column(name = "product_option_name", nullable = false)
    private String productOptionName;

    @Builder
    public Cart(User user, UUID sellingBidId,UUID productId, UUID productOptionId, String productName, String productOptionName, String productImageUrl,
            String modelNumber) {
        this.user = user;
        this.sellingBidId = sellingBidId;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.productOptionName = productOptionName;
        this.productImageUrl = productImageUrl;
        this.modelNumber = modelNumber;
    }
}
