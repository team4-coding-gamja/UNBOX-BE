package com.example.unbox_be.domain.wishlist.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_wishlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자
@SQLRestriction("deleted_at IS NULL")
public class Wishlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wishlist_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOption productOption;

    @Builder
    public Wishlist(User user, ProductOption productOption) {
        this.user = user;
        this.productOption = productOption;
    }
}