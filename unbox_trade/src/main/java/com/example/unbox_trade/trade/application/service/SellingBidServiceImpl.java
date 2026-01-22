package com.example.unbox_trade.trade.application.service;

import com.example.unbox_trade.common.client.product.ProductClient;
import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_trade.common.client.user.UserClient;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForCartInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.internal.SellingBidForOrderInfoResponse;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidsPriceUpdateRequestDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidCreateResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidDetailResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidListResponseDto;
import com.example.unbox_trade.trade.presentation.dto.response.SellingBidsPriceUpdateResponseDto;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.presentation.mapper.SellingBidMapper;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import com.example.unbox_trade.trade.presentation.mapper.TradeClientMapper;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    private final ProductClient productClient;
    private final UserClient userClient;
    private final TradeClientMapper tradeClientMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ✅ 판매 입찰 생성
    @Override
    @Transactional
    public SellingBidCreateResponseDto createSellingBid(Long sellerId, SellingBidCreateRequestDto requestDto) {
        // 1) 회원 검증 (API Call)
        userClient.getUserInfoForSellingBid(sellerId);

        // 가격 유효성 검사
        if (requestDto.getPrice() == null || requestDto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        ProductOptionForSellingBidInfoResponse productInfo = productClient.getProductOptionForSellingBid(requestDto.getProductOptionId());

        // 만료일(deadline) 30일 뒤 00시로 설정
        LocalDateTime deadline = LocalDate.now().plusDays(30).atStartOfDay();

        SellingBid sellingBid = sellingBidMapper.toEntity(requestDto, sellerId, deadline, productInfo);

        SellingBid savedBid = sellingBidRepository.save(sellingBid);

        // 🔔 최저가 갱신 이벤트 발행
        publishPriceEvent(savedBid.getProductId(), savedBid.getProductOptionId());

        return sellingBidMapper.toCreateResponseDto(savedBid);
    }

    // ✅ 판매 입찰 취소
    @Override
    @Transactional
    public void cancelSellingBid(UUID sellingId, Long userId, String deletedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // LIVE 상태만 취소 가능
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 상태 변경
        sellingBid.updateStatus(SellingStatus.CANCELLED);
        if (deletedBy != null) {
            sellingBid.updateModifiedBy(deletedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
    }

    // ✅ 판매 입찰 가격 수정
    @Override
    @Transactional
    public SellingBidsPriceUpdateResponseDto updateSellingBidPrice(UUID sellingId,
            SellingBidsPriceUpdateRequestDto requestDto, Long userId) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 가격 유효성 검사
        if (requestDto.getNewPrice() == null || requestDto.getNewPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_BID_PRICE);
        }

        // LIVE 상태만 가격 변경 가능
        if (sellingBid.getStatus() != SellingStatus.LIVE) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 엔티티 가격 업데이트 (JPA dirty checking으로 반영)
        sellingBid.updatePrice(requestDto.getNewPrice(), userId, "SYSTEM");

        // 🔔 최저가 갱신 이벤트 발행
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());

        return sellingBidMapper.toPriceUpdateResponseDto(sellingId, requestDto.getNewPrice());
    }

    // ✅ 판매 입찰 상세 조회
    @Override
    @Transactional(readOnly = true)
    public SellingBidDetailResponseDto getSellingBidDetail(UUID sellingId, Long userId) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingId)
                .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

        // 본인 소유 확인
        if (!Objects.equals(sellingBid.getSellerId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // Product 서비스 호출
        ProductOptionForSellingBidInfoResponse productInfo = productClient
                .getProductOptionForSellingBid(sellingBid.getProductOptionId());

        return sellingBidMapper.toDetailResponseDto(sellingBid, productInfo);
    }

    // ✅ 내 판매 입찰 목록 조회 (Slice)
    @Override
    @Transactional(readOnly = true)
    public Slice<SellingBidListResponseDto> getMySellingBids(Long userId, Pageable pageable) {

        Slice<SellingBid> bids = sellingBidRepository.findBySellerIdOrderByCreatedAtDesc(userId, pageable);

        return bids.map(bid -> {
            ProductOptionForSellingBidInfoResponse productInfo = productClient
                    .getProductOptionForSellingBid(bid.getProductOptionId());

            return sellingBidMapper.toListResponseDto(bid, productInfo);
        });
    }

    // ========================================
    // ✅ 내부 시스템용 API (Internal API)
    // ========================================

    // ✅ 판매 글 조회 (장바구니용)
    @Override
    @Transactional(readOnly = true)
    public SellingBidForCartInfoResponse getSellingBidForCart(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForCartInfoResponse(sellingBid);
    }

    // ✅ 판매 글 조회 (주문용)
    @Override
    @Transactional(readOnly = true)
    public SellingBidForOrderInfoResponse getSellingBidForOrder(UUID sellingBidId) {
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        return tradeClientMapper.toSellingBidForOrderInfoResponse(sellingBid);
    }

    // ✅ 판매 입찰 선점 (주문용: LIVE → RESERVED)
    @Override
    @Transactional
    public void reserveSellingBid(UUID sellingBidId, String updatedBy) {
        // 존재 여부 확인
        sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // 동시성 제어 업데이트 (LIVE 상태인 것만 RESERVED로 변경)
        int updated = sellingBidRepository.updateStatusIfReserved(
                sellingBidId,
                SellingStatus.LIVE,
                SellingStatus.RESERVED);
        // 업데이트 실패 시 예외 발생
        if (updated == 0) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        SellingBid sellingBid = sellingBidRepository.findById(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // updatedBy 기록 (선택사항)
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
    }

    // ✅ 판매 입찰 완료 처리 (결제 완료용: RESERVED → SOLD)
    @Transactional
    @Override
    public void soldSellingBid(UUID sellingBidId, String updatedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // 상태 검증 (RESERVED 상태만 SOLD로 변경 가능)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 상태 변경
        sellingBid.updateStatus(SellingStatus.SOLD);
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }

        // 🔔 최저가 갱신 이벤트 발행
        publishPriceEvent(sellingBid.getProductId(), sellingBid.getProductOptionId());
    }

    private void publishPriceEvent(UUID productId, UUID optionId) {
        BigDecimal minPrice = sellingBidRepository.findLowestPriceByOptionId(optionId)
                .orElse(BigDecimal.ZERO);
        eventPublisher.publishEvent(new TradePriceChangedEvent(productId, optionId, minPrice));
    }

    // ✅ 판매 입찰 복구 (결제 실패/취소용: RESERVED → LIVE)
    @Override
    @Transactional
    public void liveSellingBid(UUID sellingBidId, String updatedBy) {
        // 입찰 조회
        SellingBid sellingBid = sellingBidRepository.findByIdAndDeletedAtIsNull(sellingBidId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLING_BID_NOT_FOUND));
        // 상태 검증 (RESERVED 상태만 LIVE로 복구 가능)
        if (sellingBid.getStatus() != SellingStatus.RESERVED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        // 상태 변경
        sellingBid.updateStatus(SellingStatus.LIVE);
        if (updatedBy != null) {
            sellingBid.updateModifiedBy(updatedBy);
        }
    }
}