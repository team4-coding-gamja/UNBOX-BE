package com.example.unbox_be.domain.trade.entity;

import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_buying_bids")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BuyingBid extends BaseEntity {

    @Id
    @Column(name = "buying_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID buyingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BuyingStatus status = BuyingStatus.LIVE;

    private LocalDateTime deadline;
}
