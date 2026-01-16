package com.example.unbox_be.user.cart.entity;

import com.example.unbox_be.common.entity.BaseEntity;
import com.example.unbox_be.trade.domain.entity.SellingBid;
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

    @Column(name = "selling_bid_id", nullable = false)
    private UUID sellingBidId;

    // --- 상품, 상품옵션 스냅샷 ---
    private UUID productId;
    private UUID productOptionId;
    private String productName;
    private String productImageUrl;
    private String modelNumber;
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
