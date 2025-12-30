package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.trade.dto.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SellingBidService {

    private final SellingBidRepository sellingBidRepository;

    @Transactional
    public Long createSellingBid(SellingBidRequestDto requestDto) {
        // 현재 날짜 기준 30일 뒤의 00시 00분 00초 계산
        // 오늘로부터 30일 뒤 날짜의 시작 시간(00:00:00)
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        // DTO -> Entity 변환
        SellingBid sellingBid = new SellingBid(
                null,
                requestDto.getUserId(),
                requestDto.getOptionId(),
                requestDto.getPrice(),
                SellingStatus.LIVE,
                deadline
        );

        SellingBid savedBid = sellingBidRepository.save(sellingBid);
        return savedBid.getSellingId();
    }
}