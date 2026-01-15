package com.example.unbox_be.domain.trade.trade.dto.request;

import com.example.unbox_be.domain.trade.trade.entity.BuyingStatus;
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
