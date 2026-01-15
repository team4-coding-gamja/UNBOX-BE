package com.example.unbox_be.trade.dto.request;

import com.example.unbox_be.trade.entity.BuyingStatus;
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
