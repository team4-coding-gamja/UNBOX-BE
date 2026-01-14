package com.example.unbox_be.domain.reviews.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.order.entity.Order;
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

    @Column(name = "review_Image_url")
    private String reviewImageUrl;

    // 1주문 1리뷰 원칙
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    public Review(Order order, String content, Integer rating, String reviewImageUrl) {
        this.order = order;
        this.content = content;
        this.rating = rating;
        this.reviewImageUrl = reviewImageUrl;
    }

    public static Review createReview(Order order, String content, Integer rating, String imageUrl) {
        validateCreate(order, content, rating, imageUrl);
        return new Review(order, normalizeContent(content), rating, normalizeImageUrl(imageUrl));
    }

    public void update(String content, Integer rating, String reviewImageUrl) {
        validatePatchUpdate(content, rating, reviewImageUrl);

        if (content != null) {
            this.content = normalizeContent(content);
        }
        if (rating != null) {
            this.rating = rating;
        }
        if (reviewImageUrl != null) {
            String normalized = normalizeImageUrl(reviewImageUrl);
            this.reviewImageUrl = normalized;
        }
    }

    // =======================
    // Validation (Domain Rule)
    // =======================

    private static void validateCreate(Order order, String content, Integer rating, String imageUrl) {
        requireNotNull(order, "order");
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
        if (imageUrl == null) return; // 선택 값
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
        if (imageUrl == null) return null;
        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
