package com.example.unbox_product.product.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "p_brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Brand extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "brand_id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String imageUrl;

    // ======================= 연관 관계 =======================
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // ======================= 생성자 =======================
    private Brand(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    // ======================= 정적 메서드 =======================
    public static Brand createBrand(String name, String imageUrl) {
        validateName(name);
        validateLogoUrl(imageUrl);

        return new Brand(name, imageUrl);
    }

    // ======================= 유효성 검사 메서드 =======================
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("브랜드명은 50자를 초과할 수 없습니다.");
        }
    }

    private static void validateLogoUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
        throw new IllegalArgumentException("로고 URL은 필수입니다.");
        }
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new IllegalArgumentException("로고 URL은 http 또는 https 형식이어야 합니다.");
        }
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    public void updateLogoUrl(String imageUrl) {
        validateLogoUrl(imageUrl);
        this.imageUrl = imageUrl;
    }
}
