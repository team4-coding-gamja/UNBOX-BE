package com.example.unbox_be.domain.cart.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "p_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@org.hibernate.annotations.SQLDelete(sql = "UPDATE p_cart SET deleted_at = NOW() WHERE id = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL") // 조회 시 자동으로 deleted_at IS NULL 조건 추가
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

    @Builder
    public Cart(User user, SellingBid sellingBid) {
        this.user = user;
        this.sellingBid = sellingBid;
    }
}
