package com.example.unbox_be.domain.trade.controller;

import com.example.unbox_be.domain.trade.dto.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids/selling")
@RequiredArgsConstructor
public class SellingBidController {

    private final SellingBidService sellingBidService;

    @PostMapping
    public ResponseEntity<Long> createSellingBid(@RequestBody SellingBidRequestDto requestDto) {
        Long savedId = sellingBidService.createSellingBid(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }
}