package com.example.unbox_be.product.product.domain.entity;

import com.example.unbox_be.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class ProductOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_option_id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    // ======================= 연관 관계 =======================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ======================= 생성자 =======================
    private ProductOption(Product product, String name) {
        this.product = product;
        this.name = name;
    }

    // ======================= 정적 메서드 =======================
    public static ProductOption createProductOption(Product product, String name) {
        validateProduct(product);
        validateOption(name);
        return new ProductOption(product, name);
    }

    // ======================= 유효성 검사 메서드 =======================
    private static void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("상품은 필수입니다.");
        }
    }

    private static void validateOption(String option) {
        if (option == null || option.isBlank()) {
            throw new IllegalArgumentException("옵션 값은 필수입니다.");
        }
        if (option.length() > 50) {
            throw new IllegalArgumentException("옵션 값은 50자를 초과할 수 없습니다.");
        }
    }
}
