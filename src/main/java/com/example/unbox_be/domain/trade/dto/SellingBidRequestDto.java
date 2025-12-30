package com.example.unbox_be.domain.trade.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SellingBidRequestDto {
    private Long userId;
    private Long optionId;
    private Integer price;
    private Integer deadlineDays; // 며칠 동안 게시할지  -> 일단 30일로 고정시킴 1월 1일-> 1월 31일 00시
}
