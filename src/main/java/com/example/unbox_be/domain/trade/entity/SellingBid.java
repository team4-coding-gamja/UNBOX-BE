package com.example.unbox_be.domain.trade.entity;


import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_selling_bids")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SQLRestriction("deleted_at IS NULL")
public class SellingBid extends BaseEntity {

    @Id
    @Column(name = "selling_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sellingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SellingStatus status = SellingStatus.LIVE;

    private LocalDateTime deadline;

    public void cancel() {
        this.status = SellingStatus.CANCELLED;
    }

    public void updatePrice(Integer newPrice, Long userId) {
        // 1. 본인 확인
        if (!this.userId.equals(userId)) {
            throw new IllegalArgumentException("본인의 판매 입찰만 수정할 수 있습니다.");
        }

        // 2. 상태 확인 (LIVE일 때만 수정 가능)
        if (this.status != SellingStatus.LIVE) {
            throw new IllegalStateException("판매 중(LIVE) 상태일 때만 가격을 수정할 수 있습니다.");
        }

        // 3. 가격 변경
        this.price = newPrice;
    }
}
