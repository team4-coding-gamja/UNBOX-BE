package com.example.unbox_be.user.cart.entity;

import com.example.unbox_be.common.entity.BaseEntity;
import com.example.unbox_be.trade.entity.SellingBid;
import com.example.unbox_be.user.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(name = "p_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Cart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_bid_id", nullable = false)
    private SellingBid sellingBid;

    // --- 상품, 상품옵션 스냅샷 ---
    private String productName;
    private String productImageUrl;
    private String modelName;
    private String productOptionName;


    @Builder
    public Cart(User user, SellingBid sellingBid,  String productName, String productOptionName, String productImageUrl, String modelName) {
            this.user = user;
            this.sellingBid = sellingBid;
            this.productName = productName;
            this.productOptionName = productOptionName;
            this.productImageUrl = productImageUrl;
            this.modelName = modelName;
    }
}
