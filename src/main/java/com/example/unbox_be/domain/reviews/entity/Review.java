package com.example.unbox_be.domain.reviews.entity;

import com.example.unbox_be.domain.common.BaseEntity; //
import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;


@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 직접 생성 방지
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {
    // BaseEntity를 상속받아 생성/수정/삭제 자동 관리

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // PK를 UUID로 자동 생성
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID reviewId; // 상품리뷰ID (PK)

    @OneToOne(fetch = FetchType.LAZY)   // 주문ID (FK 역할)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;  // 구매자ID (FK 역할, buyer ID값만 저장)

    @Column(name = "product_id", nullable = false)
    private UUID productId; // 상품 PK, 쿼리스트링 조회용

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private Integer rating; // 평점 (1~5점)

    @Column(name = "image_url")
    private String imageUrl; // 이미지 주소


    // 리뷰 정적 메서드
    public static Review createReview(UUID productId, Order order, User buyer, String content, Integer rating, String imageUrl) {
        Review review = new Review();
        review.productId = productId;
        review.order = order;
        review.buyer = buyer;
        review.content = content;
        review.rating = rating;
        review.imageUrl = imageUrl;
        return review;
    }

    // 리뷰수정 비즈니스 로직
    public void update(String content, Integer rating, String imageUrl) {
        this.content = content;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }


}