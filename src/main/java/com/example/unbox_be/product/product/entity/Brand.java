package com.example.unbox_be.product.product.entity;

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
    private String logoUrl;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    // 생성자
    private Brand(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }

    // 생성 메서드
    public static Brand createBrand(String name, String logoUrl) {
        validateName(name);
        validateLogoUrl(logoUrl);

        return new Brand(name, logoUrl);
    }

    // 유효성 검증 메서드
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다.");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("브랜드명은 50자를 초과할 수 없습니다.");
        }
    }

    private static void validateLogoUrl(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
        throw new IllegalArgumentException("로고 URL은 필수입니다.");
        }
        if (!logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")) {
            throw new IllegalArgumentException("로고 URL은 http 또는 https 형식이어야 합니다.");
        }
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    public void updateLogoUrl(String logoUrl) {
        validateLogoUrl(logoUrl);
        this.logoUrl = logoUrl;
    }
}
