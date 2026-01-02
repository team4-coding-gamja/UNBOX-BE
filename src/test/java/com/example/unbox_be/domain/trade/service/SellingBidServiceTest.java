package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.mapper.SellingBidMapper;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
// ❌ User 관련 import 삭제 (필요 없음)
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Assertions 통합
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellingBidServiceTest {

    // ❌ UserRepository Mock 삭제
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private SellingBidRepository sellingBidRepository;
    @Mock
    private SellingBidMapper sellingBidMapper;

    @InjectMocks
    private SellingBidService sellingBidService;

    // User 객체 대신 ID만 필요
    private final Long USER_ID = 1L;
    private final String EMAIL = "test@example.com";

    private ProductOption productOption;
    private SellingBid sellingBid;
    private UUID bidId;

    @BeforeEach
    void setUp() throws Exception {
        bidId = UUID.randomUUID();

        // 1. User 생성 로직 삭제 (ID 상수 사용)

        // 2. Product 생성
        Constructor<Product> productConstructor = Product.class.getDeclaredConstructor();
        productConstructor.setAccessible(true);
        Product product = productConstructor.newInstance();
        ReflectionTestUtils.setField(product, "id", 1L); // Product ID Long으로 가정 (맞춰서 수정)
        ReflectionTestUtils.setField(product, "name", "Nike Air Max");
        ReflectionTestUtils.setField(product, "imageUrl", "nike.png");

        // 3. ProductOption 생성
        Constructor<ProductOption> optionConstructor = ProductOption.class.getDeclaredConstructor();
        optionConstructor.setAccessible(true);
        productOption = optionConstructor.newInstance();
        ReflectionTestUtils.setField(productOption, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(productOption, "option", "270");
        ReflectionTestUtils.setField(productOption, "product", product);

        // 4. SellingBid 생성
        sellingBid = SellingBid.builder()
                .sellingId(bidId)
                .userId(USER_ID) // 상수로 설정
                .productOption(productOption)
                .price(150000)
                .status(SellingStatus.LIVE)
                .deadline(LocalDateTime.now().plusDays(30))
                .build();
    }

    @Test
    @DisplayName("판매 입찰 상세 조회 성공")
    void getSellingBidDetail_Success() {
        // given
        SellingBidResponseDto mockDto = SellingBidResponseDto.builder()
                .sellingId(bidId)
                .price(150000)
                .status(SellingStatus.LIVE)
                .build();

        // ❌ UserRepository 모킹 삭제
        // given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        given(sellingBidRepository.findWithDetailsBySellingId(any(UUID.class)))
                .willReturn(Optional.of(sellingBid));

        given(sellingBidMapper.toResponseDto(any(SellingBid.class))).willReturn(mockDto);

        // when (파라미터 변경: email -> userId)
        SellingBidResponseDto result = sellingBidService.getSellingBidDetail(bidId, USER_ID);

        // then
        assertThat(result.getSellingId()).isEqualTo(bidId);
        assertThat(result.getSize()).isEqualTo("270");
        assertThat(result.getProduct().getName()).isEqualTo("Nike Air Max");
    }

    @Test
    @DisplayName("내 판매 입찰 목록 조회 성공 (Slice)")
    void getMySellingBids_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<SellingBid> bids = List.of(sellingBid);
        Slice<SellingBid> bidSlice = new SliceImpl<>(bids, pageable, false);

        SellingBidResponseDto mockDto = SellingBidResponseDto.builder()
                .sellingId(bidId)
                .price(150000)
                .build();

        // ❌ UserRepository 모킹 삭제
        // given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // Repo 호출 시 anyLong()으로 매칭
        given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
                .willReturn(bidSlice);
        given(sellingBidMapper.toResponseDto(any(SellingBid.class))).willReturn(mockDto);

        // when (파라미터 변경: email -> userId)
        Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(USER_ID, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).asList().hasSize(1);

        // 매퍼가 잘 동작했다고 가정하고 결과 검증
        SellingBidResponseDto firstDto = result.getContent().get(0);
        assertThat(firstDto.getSize()).isEqualTo("270");
        assertThat(firstDto.getProduct().getName()).isEqualTo("Nike Air Max");
    }

    @Test
    @DisplayName("입찰 가격 수정 성공")
    void updateSellingBidPrice_Success() {
        // given
        Integer newPrice = 160000;

        // ❌ UserRepository 모킹 삭제

        given(sellingBidRepository.findById(any(UUID.class))).willReturn(Optional.of(sellingBid));

        // when (파라미터 변경: userId 추가)
        sellingBidService.updateSellingBidPrice(bidId, newPrice, USER_ID, EMAIL);

        // then
        assertThat(sellingBid.getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("본인이 아닌 경우 가격 수정 시 예외 발생")
    void updateSellingBidPrice_AccessDenied() {
        // given
        Long otherUserId = 999L; // 다른 사람 ID

        // ❌ UserRepository 모킹 삭제

        given(sellingBidRepository.findById(any(UUID.class))).willReturn(Optional.of(sellingBid));

        // when & then
        assertThatThrownBy(() -> sellingBidService.updateSellingBidPrice(bidId, 160000, otherUserId, EMAIL))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}