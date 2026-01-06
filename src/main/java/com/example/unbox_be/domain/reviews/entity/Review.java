package com.example.unbox_be.domain.reviews.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@org.hibernate.annotations.SQLDelete(sql = "UPDATE p_review SET deleted_at = NOW() WHERE review_id = ?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID id;

    // 1주문 1리뷰 원칙
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    // ERD상 Product 테이블과는 연관관계(FK)가 없으므로 ID값만 저장
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "image_url")
    private String imageUrl;

    public static Review createReview(UUID productId, Order order, User buyer, String content, Integer rating, String imageUrl) {
        return Review.builder()
                .productId(productId)
                .order(order)
                .buyer(buyer)
                .content(content)
                .rating(rating)
                .imageUrl(imageUrl)
                .build();
    }

    public void update(String content, Integer rating, String imageUrl) {
        this.content = content;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }
}