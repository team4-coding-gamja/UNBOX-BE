package com.example.unbox_be.domain.reviews.entity;

import com.example.unbox_be.domain.common.BaseEntity; //
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {
    // BaseEntity를 상속받아 생성/수정일 자동 관리

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // PK를 UUID로 자동 생성
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID reviewId; // 상품리뷰ID (PK)

    @Column(name = "order_id", nullable = false)
    private UUID orderId; // 주문ID (FK 역할, ID값만 저장), 배송완료 체크용

    @Column(name = "product_id", nullable = false)
    private UUID productId; // 상품 PK, 쿼리스트링 조회용

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId; // 구매자ID (FK 역할, ID값만 저장)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 리뷰 내용

    @Column(nullable = false)
    private Integer rating; // 평점 (1~5점)

    @Column(name = "image_url")
    private String imageUrl; // 이미지 주소

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 삭제 한 날짜

    @Column(name = "deleted_by")
    private String deletedBy;  // 누가 삭제했는지

    // 리뷰수정 비즈니스 로직
    public void update(String content, Integer rating, String imageUrl) {
        this.content = content;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    // 소프트 삭제 처리를 위한 비즈니스 메서드
    public void delete(String adminOrUserId) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = adminOrUserId;
    }
}