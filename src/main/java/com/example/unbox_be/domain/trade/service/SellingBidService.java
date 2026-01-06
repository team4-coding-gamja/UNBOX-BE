package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.request.UpdateSellingStatusRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.example.unbox_be.domain.trade.mapper.SellingBidMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellingBidService {

    private final UserRepository userRepository;
    private final ProductOptionRepository productOptionRepository;
    private final SellingBidRepository sellingBidRepository;
    private final SellingBidMapper sellingBidMapper;

    @Transactional
    public UUID createSellingBid(Long userId, SellingBidRequestDto requestDto) {

        // 1. [수정] 단순히 존재 확인(exists)만 하지 말고, 실제 객체를 조회(findByIdAndDeletedAtIsNull)합니다.
        ProductOption productOption = productOptionRepository.findByIdAndDeletedAtIsNull(requestDto.getOptionId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        // 30일 뒤 00시로 deadline 설정
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        // 실제 주문 생성
        SellingBid sellingBid = sellingBidMapper.toEntity(requestDto, userId, deadline, productOption);
        // 생성된 주문 저장
        SellingBid savedBid = sellingBidRepository.save(sellingBid);
        return savedBid.getId();
    }

    @Transactional
    public void cancelSellingBid(UUID sellingId, Long userId, String email) {
        // 1. 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 2. [변경] 본인 확인 (DB 조회 없이 ID 비교만 수행)
        if (!Objects.equals(sellingBid.getUserId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        // 판매 예약 주문 LIVE 상태 확인
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new IllegalArgumentException("취소할 수 없는 상태입니다.");
        }

        updateSellingBidStatus(sellingId, SellingStatus.CANCELLED, userId, email);
    }

    @Transactional
    public void updateSellingBidPrice(UUID sellingId, Integer newPrice, Long userId, String email) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
        // 입력 가격 유효성 검사
        if (newPrice == null || newPrice <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }
        // 해당 예약 상태 검사
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new IllegalArgumentException("수정할 수 없는 상태입니다.");
        }

        // 로그인 유저와 해당 예약 유저 동일성 검사
        if (!Objects.equals(sellingBid.getUserId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        // 업데이트
        sellingBid.updatePrice(newPrice, userId, email);
    }

    @Transactional(readOnly = true)
    public SellingBidResponseDto getSellingBidDetail(UUID sellingId, Long userId) {
        // User 조회 삭제

        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId) // 이걸로 변경!
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 로그인 유저와 해당 예약 유저 동일성 검사
        if (!Objects.equals(sellingBid.getUserId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        // 동일하다면 sellingBid 반환 객체 생성
        SellingBidResponseDto response = sellingBidMapper.toResponseDto(sellingBid);
        ProductOption option = sellingBid.getProductOption();

        // 객체 반환
        return SellingBidResponseDto.builder()
                .id(response.getId())
                .status(response.getStatus())
                .price(response.getPrice())
                .deadline(response.getDeadline())
                .size(option.getOption())
                .product(SellingBidResponseDto.ProductInfo.builder()
                        .id(option.getProduct().getId())
                        .name(option.getProduct().getName())
                        .imageUrl(option.getProduct().getImageUrl())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public Slice<SellingBidResponseDto> getMySellingBids(Long userId, Pageable pageable) {
        // 유저 정보 가져옴
        Slice<SellingBid> bidSlice = sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // 3. 변환 (동일)
        return bidSlice.map(bid -> {
            SellingBidResponseDto dto = sellingBidMapper.toResponseDto(bid);

            ProductOption option= bid.getProductOption();
            if (option == null) {
                return dto;
            }
            return dto.toBuilder()
                    .size(option.getOption())
                    .product(SellingBidResponseDto.ProductInfo.builder()
                            .id(option.getProduct().getId())
                            .name(option.getProduct().getName())
                            .imageUrl(option.getProduct().getImageUrl())
                            .build())
                    .build();
        });
    }

    //판매 상태 변환. 나중에 MSA로 변환하면 API로 따로 관리
    @Transactional
    public void updateSellingBidStatus(UUID sellingId, SellingStatus newStatus, Long userId, String email) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
        // 유저 정보 검사
        if (userId != null) {
            if (!Objects.equals(sellingBid.getUserId(), userId)) {
                throw new CustomException(ErrorCode.ACCESS_DENIED);
            }
        }

        // 상태 변환 괜찮은지 확인
        validateTransition(sellingBid.getStatus(), newStatus);

        // 상태 변경 및 기록
        sellingBid.updateStatus(newStatus);
        if (email != null) sellingBid.updateModifiedBy(email);
    }

    // 변환할 수 있을 sellingBID인지 검사
    private void validateTransition(SellingStatus current, SellingStatus next) {
        if ((current == SellingStatus.CANCELLED || current == SellingStatus.MATCHED)
                && next == SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}