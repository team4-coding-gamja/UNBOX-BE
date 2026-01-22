package com.example.unbox_product.reviews.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_review")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Review extends BaseEntity {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_IMAGE_URL_LENGTH = 2048;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "image_url")
    private String imageUrl;

    // ======================= ID 참조 =======================
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    // ======================= 작성자 스냅샷 =======================
    @Column(name = "buyer_nickname", nullable = false)
    private String buyerNickname;

    // ======================= 상품 스냅샷 =======================
    @Embedded
    private ReviewProductSnapshot productSnapshot;

    // ======================= 생성자 =======================
    public Review(UUID orderId, String buyerNickname, String content, Integer rating, String imageUrl,
            ReviewProductSnapshot productSnapshot) {
        this.orderId = orderId;
        this.buyerNickname = buyerNickname;
        this.content = content;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.productSnapshot = productSnapshot;
    }

    // ======================= 정적 메서드 =======================
    public static Review createReview(UUID orderId, String buyerNickname, String content, Integer rating,
            String imageUrl,
            ReviewProductSnapshot snapshot) {
        validateCreate(orderId, buyerNickname, content, rating, imageUrl);
        requireNotNull(snapshot, "productSnapshot");
        return new Review(orderId, buyerNickname, normalizeContent(content), rating, normalizeImageUrl(imageUrl),
                snapshot);
    }

    public void update(String content, Integer rating, String imageUrl) {
        validatePatchUpdate(content, rating, imageUrl);

        if (content != null) {
            this.content = normalizeContent(content);
        }
        if (rating != null) {
            this.rating = rating;
        }
        if (imageUrl != null) {
            String normalized = normalizeImageUrl(imageUrl);
            this.imageUrl = normalized;
        }
    }

    // ======================= 유효성 검사 메서드 =======================
    private static void validateCreate(UUID orderId, String buyerNickname, String content, Integer rating,
            String imageUrl) {
        requireNotNull(orderId, "order");
        requireNotNull(buyerNickname, "buyerNickname");
        validateContent(content);
        validateRating(rating);
        validateImageUrl(imageUrl);
    }

    private static void validatePatchUpdate(String content, Integer rating, String imageUrl) {
        if (content != null) {
            validateContent(content);
        }
        if (rating != null) {
            validateRating(rating);
        }
        if (imageUrl != null) {
            validateImageUrl(imageUrl);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content는 최대 " + MAX_CONTENT_LENGTH + "자까지 가능합니다.");
        }
    }

    private static void validateRating(Integer rating) {
        if (rating == null) {
            throw new IllegalArgumentException("rating은 필수입니다.");
        }
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("rating은 " + MIN_RATING + "~" + MAX_RATING + " 범위여야 합니다.");
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null)
            return; // 선택 값
        if (imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl은 공백일 수 없습니다. 없으면 null로 보내세요.");
        }
        if (imageUrl.length() > MAX_IMAGE_URL_LENGTH) {
            throw new IllegalArgumentException("imageUrl이 너무 깁니다. (최대 " + MAX_IMAGE_URL_LENGTH + ")");
        }
        // 정규식 검증은 선택:
        // - S3 URL/CloudFront 등 다양한 형태가 있어서 과하게 제한하면 운영에서 막힐 수 있음
    }

    private static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }

    private static String normalizeContent(String content) {
        return content == null ? null : content.trim();
    }

    private static String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null)
            return null;
        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
