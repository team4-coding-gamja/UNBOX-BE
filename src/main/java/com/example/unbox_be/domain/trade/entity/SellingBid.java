package com.example.unbox_be.domain.trade.entity;


import com.example.unbox_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "p_selling_bids")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SellingBid extends BaseEntity {

    @Id
    @Column(name = "selling_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sellingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SellingStatus status = SellingStatus.LIVE;

    private LocalDateTime deadline;

}
