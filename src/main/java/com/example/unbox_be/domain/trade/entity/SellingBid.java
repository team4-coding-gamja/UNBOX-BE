package com.example.unbox_be.domain.trade.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "p_selling_bids",
        indexes = {
                // [핵심] 1.사이즈별 -> 2.판매중인것 -> 3.가격낮은순
                @Index(name = "idx_selling_option_status_price", columnList = "option_id, status, price")
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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id") // 기존 컬럼 유지 시
    private ProductOption productOption;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder.Default // 중요: 빌더 사용 시에도 LIVE가 기본값으로 들어가도록 설정
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SellingStatus status = SellingStatus.LIVE;

    private LocalDateTime deadline;

    public void updatePrice(Integer newPrice, Long userId, String email) {
        // 본인 확인: 요청한 유저 ID와 입찰 생성자 ID 비교
        if (!this.userId.equals(userId)) {
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