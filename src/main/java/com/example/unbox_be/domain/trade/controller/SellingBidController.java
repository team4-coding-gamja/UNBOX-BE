package com.example.unbox_be.domain.trade.controller;

import com.example.unbox_be.domain.trade.dto.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bids/selling")
@RequiredArgsConstructor
public class SellingBidController {

    private final SellingBidService sellingBidService;

    //판매 주문 /api/bids/selling
    @PostMapping
    public ResponseEntity<UUID> createSellingBid(@Valid @RequestBody SellingBidRequestDto requestDto) {
        UUID savedId = sellingBidService.createSellingBid(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }
}