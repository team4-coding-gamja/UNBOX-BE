package com.example.unbox_be.trade.application.service;

import com.example.unbox_be.product.product.infrastructure.adapter.ProductClientAdapter;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_be.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_be.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_be.trade.domain.entity.SellingBid;
import com.example.unbox_be.trade.domain.entity.SellingStatus;
import com.example.unbox_be.trade.presentation.mapper.SellingBidMapper;
import com.example.unbox_be.trade.domain.repository.SellingBidRepository;

import com.example.unbox_be.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellingBidServiceImpl implements SellingBidService {

    private final SellingBidRepository sellingBidRepository;
    private final SellingBidMapper sellingBidMapper;
    private final ProductClientAdapter productClientAdapter;

    // ✅ 판매 입찰 생성
    @Override
    @Transactional
    public SellingBidCreateResponseDto createSellingBid(Long sellerId, SellingBidCreateRequestDto requestDto) {

        validatePrice(requestDto.getPrice());

        ProductOptionForSellingBidInfoResponse productInfo = productClientAdapter.getProductOptionForSellingBid(requestDto.getProductOptionId());

        // 만료일(deadline) 30일 뒤 00시로 설정
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        SellingBid sellingBid = sellingBidMapper.toEntity(requestDto, sellerId, deadline, productInfo);

        SellingBid savedBid = sellingBidRepository.save(sellingBid);
        return sellingBidMapper.toCreateResponseDto(savedBid);
    }

    // ✅ 판매 입찰 취소
    @Override
    @Transactional
    public void cancelSellingBid(UUID sellingId, Long userId, String deletedBy) {

        // 1) 입찰 존재 여부 조회
        SellingBid sellingBid = findSellingBidOrThrow(sellingId);

        // 2) 본인 소유 확인
        validateOwner(sellingBid, userId);

        // 3) LIVE 상태만 취소 가능
        validateLiveStatus(sellingBid);

        // 4) 상태 변경 + 수정자 기록
        processUpdateStatus(sellingBid, SellingStatus.CANCELLED, deletedBy);
    }

    // ✅ 판매 입찰 가격 수정
    @Override
    @Transactional
    public SellingBidsPriceUpdateResponseDto updateSellingBidPrice(UUID sellingId,
            SellingBidsPriceUpdateRequestDto requestDto, Long userId) {

        // 1) 입찰 존재 여부 조회
        SellingBid sellingBid = findSellingBidOrThrow(sellingId);

        // 2) 본인 소유 확인
        validateOwner(sellingBid, userId);

        // 3) 가격 유효성 검사
        validatePrice(requestDto.getNewPrice());

        // 4) LIVE 상태만 가격 변경 가능
        validateLiveStatus(sellingBid);

        // 5) 엔티티 가격 업데이트 (JPA dirty checking으로 반영)
        // Note: updatePrice는 엔티티 내부에서 본인 확인을 다시 하므로 userId와 email 필요
        sellingBid.updatePrice(requestDto.getNewPrice(), userId, "SYSTEM");

        // 6) 변경된 가격을 담은 응답 DTO 반환
        return sellingBidMapper.toPriceUpdateResponseDto(sellingId, requestDto.getNewPrice());
    }

    // ✅ 판매 입찰 상세 조회
    @Override
    @Transactional(readOnly = true)
    public SellingBidDetailResponseDto getSellingBidDetail(UUID sellingId, Long userId) {

        SellingBid sellingBid = findSellingBidOrThrow(sellingId);
        validateOwner(sellingBid, userId);

        // ✅ optionId 기준으로 Product 서비스 호출
        ProductOptionForSellingBidInfoResponse productInfo = productClientAdapter
                .getProductOptionForSellingBid(sellingBid.getProductOptionId());

        return sellingBidMapper.toDetailResponseDto(sellingBid, productInfo);
    }

    // ✅ 내 판매 입찰 목록 조회 (Slice)
    @Override
    @Transactional(readOnly = true)
    public Slice<SellingBidListResponseDto> getMySellingBids(Long userId, Pageable pageable) {

        Slice<SellingBid> bids = sellingBidRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable);

        return bids.map(bid -> {
            ProductOptionForSellingBidInfoResponse productInfo = productClientAdapter
                    .getProductOptionForSellingBid(bid.getProductOptionId());

            return sellingBidMapper.toListResponseDto(bid, productInfo);
        });
    }

    // ✅ [내부 시스템용] 상태 변경
    @Transactional
    public void updateSellingBidStatusBySystem(UUID sellingId, SellingStatus newStatus, String modifiedBy) {

        // 입찰 조회
        SellingBid sellingBid = findSellingBidOrThrow(sellingId);

        // 상태 변경 처리
        processUpdateStatus(sellingBid, newStatus, modifiedBy);
    }

    /*
     * =========================================================
     * ✅ Private Helpers (검증/조회/상태변경 공통 로직)
     * =========================================================
     */

    // 가격 유효성 검사 (null, 0 이하 금지)
    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }
    }

    // LIVE 상태인지 검사 (LIVE 아니면 수정/취소 불가)
    private void validateLiveStatus(SellingBid sellingBid) {
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

    // 소유자 검사: 요청 userId와 입찰 userId가 같아야 함
    private void validateOwner(SellingBid sellingBid, Long userId) {
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    // 상태 전이(transition) 검증: 특정 상태에서 LIVE로 되돌아가는 것 금지 등
    private void validateTransition(SellingStatus current, SellingStatus next) {
        if ((current == SellingStatus.CANCELLED || current == SellingStatus.MATCHED)
                && next == SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    // 실제 상태 변경 처리 (검증 + 엔티티 수정 + 수정자 기록)
    private void processUpdateStatus(SellingBid sellingBid, SellingStatus newStatus, String modifiedBy) {
        validateTransition(sellingBid.getStatus(), newStatus); // 상태 전이 가능 여부 확인
        sellingBid.updateStatus(newStatus); // 상태 변경
        if (modifiedBy != null) { // 수정자 기록(선택)
            sellingBid.updateModifiedBy(modifiedBy);
        }
    }

    // 삭제되지 않은 SellingBid만 조회, 없으면 예외
    private SellingBid findSellingBidOrThrow(UUID sellingId) {
        return sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));
    }

    /**
     * ✅ [Internal System] 엔티티 조회 (다른 도메인에서 내부적으로 사용할 수 있음)
     */
    @Transactional(readOnly = true)
    public SellingBid findSellingBidById(UUID sellingId) {
        return findSellingBidOrThrow(sellingId);
    }
}