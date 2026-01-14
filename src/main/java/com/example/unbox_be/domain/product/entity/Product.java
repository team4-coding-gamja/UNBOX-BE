package com.example.unbox_be.domain.product.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "model_number")
    private String modelNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(columnDefinition = "integer default 0", nullable = false)
    private int reviewCount = 0;

    @Column(columnDefinition = "integer default 0", nullable = false)
    private int totalScore = 0;

    // 같은 도메인 내부이므로 연관관계 유지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    private Product(String name, String modelNumber, Category category, String productImageUrl, Brand brand, int reviewCount,
            int totalScore) {
        this.name = name;
        this.modelNumber = modelNumber;
        this.category = category;
        this.productImageUrl = productImageUrl;
        this.brand = brand;
        this.reviewCount = reviewCount;
        this.totalScore = totalScore;
    }

    // 생성 메서드
    public static Product createProduct(String name, String modelNumber, Category category, String imageUrl,
            Brand brand) {
        validateName(name);
        validateCategory(category);
        validateBrand(brand);
        validateImageUrl(imageUrl);

        return new Product(name, modelNumber, category, imageUrl, brand, 0, 0);
    }

    public void update(String name, String modelNumber, Category category, String productImageUrl) {
        validateName(name);
        validateModelNumber(modelNumber);
        validateCategory(category);
        validateImageUrl(productImageUrl);

        this.name = name;
        this.modelNumber = modelNumber;
        this.category = category;
        this.productImageUrl = productImageUrl;
    }

    // 유효성 검증 메서드
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자를 초과할 수 없습니다.");
        }
    }

    private static void validateModelNumber(String modelNumber) {
        if (modelNumber == null || modelNumber.isBlank()) {
            throw new IllegalArgumentException("상품번호는 필수입니다.");
        }
    }

    private static void validateCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
    }

    private static void validateBrand(Brand brand) {
        if (brand == null) {
            throw new IllegalArgumentException("브랜드는 필수입니다.");
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return; // 이미지 선택 사항이면 통과
        }
        if (!imageUrl.startsWith("http")) {
            throw new IllegalArgumentException("이미지 URL은 http 또는 https 형식이어야 합니다.");
        }
    }

    public void addReviewData(int rating) {
        this.reviewCount++;
        this.totalScore += rating;
    }

    public void deleteReviewData(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
        if (this.reviewCount == 0) {
            throw new IllegalStateException("삭제할 리뷰가 없습니다.");
        }
        this.reviewCount--;
        this.totalScore -= rating;
        if (this.totalScore < 0)
            this.totalScore = 0;
    }

    public void updateReviewData(int oldRating, int newRating) {
        if (oldRating < 1 || oldRating > 5) {
            throw new IllegalArgumentException("이전 평점은 1에서 5 사이여야 합니다.");
        }
        if (newRating < 1 || newRating > 5) {
            throw new IllegalArgumentException("새 평점은 1에서 5 사이여야 합니다.");
        }
        this.totalScore -= oldRating;
        this.totalScore += newRating;
        if (this.totalScore < 0)
            this.totalScore = 0;
    }
}
