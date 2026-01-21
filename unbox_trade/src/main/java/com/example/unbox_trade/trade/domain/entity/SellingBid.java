package com.example.unbox_trade.trade.domain.entity;

import com.example.unbox_common.entity.BaseEntity;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "p_selling_bids",
        indexes = {
                // [핵심] 1.사이즈별 -> 2.판매중인것 -> 3.가격낮은순
                @Index(name = "idx_selling_option_status_price", columnList = "product_option_id, status, price")
        })
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class SellingBid extends BaseEntity {

    @Id
    @Column(name = "selling_id")
    @NotNull
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ======================= ID 참조 =======================
    @Column(name = "user_id", nullable = false)
    private Long sellerId;

    @Column(name = "product_option_id", nullable = false)
    private UUID productOptionId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // ======================= 판매입찰 정보 =======================
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Builder.Default // 중요: 빌더 사용 시에도 LIVE가 기본값으로 들어가도록 설정
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SellingStatus status = SellingStatus.LIVE;

    private LocalDateTime deadline;

    // ======================= 상품 스냅샷 =======================
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "model_number", nullable = false)
    private String modelNumber;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "product_option_name", nullable = false)
    private String productOptionName;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    // ======================= 비즈니스 로직 메서드 =======================
    public void updatePrice(BigDecimal newPrice, Long sellerId, String email) {
        // 본인 확인: 요청한 유저 ID와 입찰 생성자 ID 비교
        if (!this.sellerId.equals(sellerId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 상태 확인: 이미 판매 완료되었거나 취소된 경우 수정 불가
        if (this.status != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.price = newPrice;
        this.updateModifiedBy(email);
    }

    public void updateStatus(SellingStatus status) {
        if(status == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.status = status;
    }
}