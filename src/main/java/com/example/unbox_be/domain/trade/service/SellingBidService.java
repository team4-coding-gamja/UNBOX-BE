package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.dto.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellingBidService {

    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;
    private final SellingBidRepository sellingBidRepository;

    @Transactional
    public UUID createSellingBid(SellingBidRequestDto requestDto) {
        // 현재 날짜 기준 30일 뒤의 00시 00분 00초 계산
        // 오늘로부터 30일 뒤 날짜의 시작 시간(00:00:00) - 1월 1일->1월 31일 00시
        if (!userRepository.existsById(requestDto.getUserId())) {
            throw new EntityNotFoundException("해당 유저를 찾을 수 없습니다. ID: " + requestDto.getUserId());
        }
        if (!productOptionRepository.existsById(requestDto.getOptionId())) {
            throw new EntityNotFoundException("해당 상품 옵션을 찾을 수 없습니다. ID: " + requestDto.getOptionId());
        }
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