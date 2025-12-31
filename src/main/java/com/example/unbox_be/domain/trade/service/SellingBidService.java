package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
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
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
// 상품 옵션 확인
        if (!productOptionRepository.existsById(requestDto.getOptionId())) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
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

    @Transactional
    public void cancelSellingBid(UUID sellingId, String email) {
        // 1. 해당 입찰 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        SellingBid sellingBid = sellingBidRepository.findById(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 2. 본인 확인
        if (!sellingBid.getUserId().equals(user.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new IllegalArgumentException("아직 판매, 취소되지 않거나 구매중이 아닌 상품만 취소할 수 있습니다.");
        }
        sellingBid.softDelete(user.getEmail());

        // 3. 상태 변경 (Dirty Checking에 의해 자동 업데이트)
        sellingBid.cancel();
    }

    @Transactional
    public void updateSellingBidPrice(UUID sellingId, Integer newPrice, String email) {
        // 1. 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        SellingBid sellingBid = sellingBidRepository.findById(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
        if (newPrice == null || newPrice <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new IllegalArgumentException("아직 판매, 취소되지 않거나 구매중이 아닌 상품만 취소할 수 있습니다.");
        }
        // 2. 엔티티의 비즈니스 로직 호출 (검증 및 변경)
        sellingBid.updatePrice(newPrice, user.getId(), email);
    }
}