package com.example.unbox_user.user.request.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_product_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class ProductRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_request_id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String brandName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductRequestStatus status;

    // 생성자
    private ProductRequest(Long userId, String name, String brandName, ProductRequestStatus status) {
        this.userId = userId;
        this.name = name;
        this.brandName = brandName;
        this.status = status;
    }

    // 요청 생성 메서드 (유일한 생성 진입점)
    public static ProductRequest createProductRequest(Long userId, String name, String brandName) {
        validateUserId(userId);
        validateName(name);
        validateBrandName(brandName);

        return new ProductRequest(
                userId,
                name,
                brandName,
                ProductRequestStatus.PENDING
        );
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("요청자 userId는 필수입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자를 초과할 수 없습니다.");
        }
    }

    private static void validateBrandName(String brandName) {
        if (brandName == null || brandName.isBlank()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다.");
        }
        if (brandName.length() > 50) {
            throw new IllegalArgumentException("브랜드명은 50자를 초과할 수 없습니다.");
        }
    }

    public void updateStatus(ProductRequestStatus status) {
        this.status = status;
    }
}
