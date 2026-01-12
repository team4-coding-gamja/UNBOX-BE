package com.example.unbox_be.domain.reviews.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
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

    // (선택) 길이 제한: 운영 정책에 맞게 조정 가능
    private static final int MAX_NICKNAME_LENGTH = 50;
    private static final int MAX_PRODUCT_NAME_LENGTH = 200;
    private static final int MAX_OPTION_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id", updatable = false, nullable = false)
    private UUID id;

    // 🔑 연결 키 (Order 엔티티 참조 X)
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    // ===== 리뷰 본문 =====
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "image_url")
    private String imageUrl;

    // ===== 스냅샷 필드 =====

    // 주문 정보
    @Column(nullable = false)
    private BigDecimal orderPrice;

    // 구매자 정보
    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false, length = MAX_NICKNAME_LENGTH)
    private String buyerNickname;

    // 상품 정보
    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, length = MAX_PRODUCT_NAME_LENGTH)
    private String productName;

    @Column(name = "product_image_url")
    private String productImageUrl;

    // 상품 옵션 정보
    @Column(nullable = false)
    private UUID productOptionId;

    @Column(nullable = false, length = MAX_OPTION_NAME_LENGTH)
    private String productOptionName;

    // ============================================
    // Factory Method (스냅샷 포함 생성 메서드만 유지)
    // ============================================
    public static Review createReview(
            UUID orderId,
            String content,
            Integer rating,
            String imageUrl,
            BigDecimal orderPrice,
            Long buyerId,
            String buyerNickname,
            UUID productId,
            String productName,
            String productImageUrl,
            UUID productOptionId,
            String productOptionName
    ) {
        validateCreate(
                orderId,
                content,
                rating,
                imageUrl,
                orderPrice,
                buyerId,
                buyerNickname,
                productId,
                productName,
                productImageUrl,
                productOptionId,
                productOptionName
        );

        Review review = new Review();

        review.orderId = orderId;
        review.content = normalizeContent(content);
        review.rating = rating;
        review.imageUrl = normalizeImageUrl(imageUrl);

        // snapshot
        review.orderPrice = orderPrice;
        review.buyerId = buyerId;
        review.buyerNickname = buyerNickname.trim();
        review.productId = productId;
        review.productName = productName.trim();
        review.productImageUrl = normalizeImageUrl(productImageUrl);
        review.productOptionId = productOptionId;
        review.productOptionName = productOptionName.trim();

        return review;
    }

    // ✅ 수정: 리뷰 본문만 수정 가능 (스냅샷은 수정 금지)
    public void update(String content, Integer rating, String imageUrl) {
        validatePatchUpdate(content, rating, imageUrl);

        if (content != null) {
            this.content = normalizeContent(content);
        }
        if (rating != null) {
            this.rating = rating;
        }
        if (imageUrl != null) {
            this.imageUrl = normalizeImageUrl(imageUrl);
        }
    }

    // =======================
    // Validation (Domain Rule)
    // =======================

    private static void validateCreate(
            UUID orderId,
            String content,
            Integer rating,
            String imageUrl,
            BigDecimal orderPrice,
            Long buyerId,
            String buyerNickname,
            UUID productId,
            String productName,
            String productImageUrl,
            UUID productOptionId,
            String productOptionName
    ) {
        requireNotNull(orderId, "orderId");
        validateContent(content);
        validateRating(rating);
        validateImageUrl(imageUrl);

        // snapshot 필수값 검증
        requireNotNull(orderPrice, "orderPrice");
        requireNotNull(buyerId, "buyerId");
        requireNotBlank(buyerNickname, "buyerNickname", MAX_NICKNAME_LENGTH);

        requireNotNull(productId, "productId");
        requireNotBlank(productName, "productName", MAX_PRODUCT_NAME_LENGTH);
        validateImageUrl(productImageUrl); // 선택값이면 null 허용

        requireNotNull(productOptionId, "productOptionId");
        requireNotBlank(productOptionName, "productOptionName", MAX_OPTION_NAME_LENGTH);
    }

    private static void validatePatchUpdate(String content, Integer rating, String imageUrl) {
        if (content != null) validateContent(content);
        if (rating != null) validateRating(rating);
        if (imageUrl != null) validateImageUrl(imageUrl);
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
        // URL 정규식 검증은 운영환경(S3/CloudFront 등) 때문에 과하게 제한하면 역효과 날 수 있어 선택.
    }

    private static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }

    private static void requireNotBlank(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "는 최대 " + maxLength + "자까지 가능합니다.");
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