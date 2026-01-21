package com.example.unbox_trade.trade.presentation.dto.request;

import com.example.unbox_trade.trade.domain.entity.BuyingStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BuyingBidSearchCondition {
    private BuyingStatus status;
    private String productName;
    private String brandName;
    private LocalDate startDate;
    private LocalDate endDate;
}
