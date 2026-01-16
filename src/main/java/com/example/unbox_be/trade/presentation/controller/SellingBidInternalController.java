package com.example.unbox_be.trade.presentation.controller;

import com.example.unbox_be.trade.application.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bids/selling")
@RequiredArgsConstructor
public class SellingBidInternalController {

    private final SellingBidService sellingBidService;

//    // ✅ 최저가 일괄 조회
//    @GetMapping("/lowest-prices")
//    public CustomApiResponse<Map<UUID, Integer>> getLowestPrices(@RequestParam("productOptionIds") List<UUID> productOptionIds) {
//        return sellingBidService.getLowestPricesByProductOptionIds(productOptionIds);
//    }
}
