package com.example.unbox_be.domain.cart.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.user.entity.User;
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_bid_id", nullable = false)
    private SellingBid sellingBid;

    @Builder
    public Cart(Long userId, SellingBid sellingBid) {
        this.userId = userId;
        this.sellingBid = sellingBid;
    }
}
