package com.example.unbox_trade.trade.application.service;

import com.example.unbox_common.event.trade.TradePriceChangedEvent;
import com.example.unbox_trade.common.client.product.ProductClient;
import com.example.unbox_trade.common.client.product.dto.ProductOptionForSellingBidInfoResponse;
import com.example.unbox_trade.trade.domain.entity.SellingBid;
import com.example.unbox_trade.trade.domain.entity.SellingStatus;
import com.example.unbox_trade.trade.domain.repository.SellingBidRepository;
import com.example.unbox_trade.trade.presentation.dto.request.SellingBidCreateRequestDto;
import com.example.unbox_trade.trade.presentation.mapper.SellingBidMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellingBidServiceImplTest {

    @InjectMocks
    private SellingBidServiceImpl sellingBidService;

    @Mock
    private SellingBidRepository sellingBidRepository;
    @Mock
    private SellingBidMapper sellingBidMapper;
    @Mock
    private ProductClient productClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void createSellingBid_shouldPublishEvent() {
        // Given
        UUID productId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(10000);

        SellingBidCreateRequestDto requestDto = mock(SellingBidCreateRequestDto.class);
        when(requestDto.getPrice()).thenReturn(price);
        when(requestDto.getProductOptionId()).thenReturn(optionId);

        ProductOptionForSellingBidInfoResponse productInfo = mock(ProductOptionForSellingBidInfoResponse.class);
        when(productClient.getProductOptionForSellingBid(optionId)).thenReturn(productInfo);

        SellingBid savedBid = mock(SellingBid.class);
        when(savedBid.getProductId()).thenReturn(productId);
        when(savedBid.getProductOptionId()).thenReturn(optionId);
        when(sellingBidMapper.toEntity(any(), anyLong(), any(), any())).thenReturn(savedBid);
        when(sellingBidRepository.save(any())).thenReturn(savedBid);

        when(sellingBidRepository.findLowestPriceByOptionId(optionId)).thenReturn(Optional.of(price));

        // When
        sellingBidService.createSellingBid(1L, requestDto);

        // Then
        verify(eventPublisher).publishEvent(any(TradePriceChangedEvent.class));
    }

    @Test
    void cancelSellingBid_shouldPublishEvent() {
        // Given
        UUID bidId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        Long userId = 1L;

        SellingBid bid = SellingBid.builder()
                .id(bidId)
                .sellerId(userId)
                .productId(productId)
                .productOptionId(optionId)
                .status(SellingStatus.LIVE)
                .price(BigDecimal.valueOf(10000))
                .build();

        when(sellingBidRepository.findByIdAndDeletedAtIsNull(bidId)).thenReturn(Optional.of(bid));
        when(sellingBidRepository.findLowestPriceByOptionId(optionId))
                .thenReturn(Optional.of(BigDecimal.valueOf(12000)));

        // When
        sellingBidService.cancelSellingBid(bidId, userId, "user");

        // Then
        verify(eventPublisher).publishEvent(any(TradePriceChangedEvent.class));
    }
}
