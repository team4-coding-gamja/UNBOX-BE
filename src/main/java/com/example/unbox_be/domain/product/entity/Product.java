package com.example.unbox_be.domain.product.entity;

import com.example.unbox_be.domain.product.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID id;

    // 브랜드와의 관계 설정 (N : 1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false)
    private String name;

    @Column(name = "product_number")
    private String modelNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "image_url")
    private String imageUrl;

    public Product(Brand brand, String name, String modelNumber, Category category, String imageUrl) {
        this.brand = brand;
        this.name = name;
        this.modelNumber = modelNumber;
        this.category = category;
        this.imageUrl = imageUrl;
    }


}
