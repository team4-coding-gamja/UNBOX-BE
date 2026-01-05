package com.example.unbox_be.domain.product.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    private Product(String name, String modelNumber, Category category, String imageUrl,Brand brand) {
        this.name = name;
        this.modelNumber = modelNumber;
        this.category = category;
        this.imageUrl = imageUrl;
        this.brand = brand;
    }
    // 생성 메서드
    public static Product createProduct(String name, String modelNumber, Category category, String imageUrl, Brand brand) {
        validateName(name);
        validateCategory(category);
        validateBrand(brand);
        validateImageUrl(imageUrl);

        return new Product(name, modelNumber, category, imageUrl, brand);
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
}
