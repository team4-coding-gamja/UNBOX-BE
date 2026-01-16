package com.example.unbox_be.trade.entity;

import com.example.unbox_common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_buying_bids")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class BuyingBid extends BaseEntity {

    @Id
    @Column(name = "buying_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BuyingStatus status = BuyingStatus.LIVE;

    private LocalDateTime deadline;
}
