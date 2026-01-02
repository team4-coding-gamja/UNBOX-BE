package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.mapper.SellingBidMapper;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
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

// Assertions 통합 (충돌 방지)
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellingBidServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private SellingBidRepository sellingBidRepository;
    @Mock
    private SellingBidMapper sellingBidMapper;

    @InjectMocks
    private SellingBidService sellingBidService;

    private User user;
    private ProductOption productOption;
    private SellingBid sellingBid;
    private UUID bidId;

    @BeforeEach
    void setUp() throws Exception {
        bidId = UUID.randomUUID();

        // 1. User 생성 (protected 생성자 접근 및 필드 세팅)
        Constructor<User> userConstructor = User.class.getDeclaredConstructor();
        userConstructor.setAccessible(true);
        user = userConstructor.newInstance();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "email", "test@example.com");

        // 2. Product 생성 (UUID 타입 ID 세팅)
        Constructor<Product> productConstructor = Product.class.getDeclaredConstructor();
        productConstructor.setAccessible(true);
        Product product = productConstructor.newInstance();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
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
                .userId(user.getId())
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
        String email = "test@example.com";
        SellingBidResponseDto mockDto = SellingBidResponseDto.builder()
                .sellingId(bidId)
                .price(150000)
                .status(SellingStatus.LIVE)
                .build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

        // 어떤 UUID가 들어와도 sellingBid를 반환하도록 설정 (findById와 커스텀 메서드 둘 다 대응)
        given(sellingBidRepository.findWithDetailsBySellingId(any(UUID.class))) // 서비스와 이름 맞춤
                .willReturn(Optional.of(sellingBid));

        given(sellingBidMapper.toResponseDto(any(SellingBid.class))).willReturn(mockDto);

        // when
        SellingBidResponseDto result = sellingBidService.getSellingBidDetail(bidId, email);

        // then
        assertThat(result.getSellingId()).isEqualTo(bidId);
        assertThat(result.getSize()).isEqualTo("270");
        assertThat(result.getProduct().getName()).isEqualTo("Nike Air Max");
    }

    @Test
    @DisplayName("내 판매 입찰 목록 조회 성공 (Slice)")
    void getMySellingBids_Success() {
        // given
        String email = "test@example.com";
        Pageable pageable = PageRequest.of(0, 10);
        List<SellingBid> bids = List.of(sellingBid);
        Slice<SellingBid> bidSlice = new SliceImpl<>(bids, pageable, false);

        SellingBidResponseDto mockDto = SellingBidResponseDto.builder()
                .sellingId(bidId)
                .price(150000)
                .build();

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(anyLong(), any(Pageable.class))).willReturn(bidSlice);
        given(sellingBidMapper.toResponseDto(any(SellingBid.class))).willReturn(mockDto);

        // when
        Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(email, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).asList().hasSize(1);

        SellingBidResponseDto firstDto = result.getContent().get(0);
        assertThat(firstDto.getSize()).isEqualTo("270");
        assertThat(firstDto.getProduct().getName()).isEqualTo("Nike Air Max");
    }

    @Test
    @DisplayName("입찰 가격 수정 성공")
    void updateSellingBidPrice_Success() {
        // given
        String email = "test@example.com";
        Integer newPrice = 160000;
        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(sellingBidRepository.findById(any(UUID.class))).willReturn(Optional.of(sellingBid));

        // when
        sellingBidService.updateSellingBidPrice(bidId, newPrice, email);

        // then
        assertThat(sellingBid.getPrice()).isEqualTo(newPrice);
    }

    @Test
    @DisplayName("본인이 아닌 경우 가격 수정 시 예외 발생")
    void updateSellingBidPrice_AccessDenied() throws Exception {
        // given
        String email = "other@example.com";

        Constructor<User> userConstructor = User.class.getDeclaredConstructor();
        userConstructor.setAccessible(true);
        User otherUser = userConstructor.newInstance();
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        ReflectionTestUtils.setField(otherUser, "email", email);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(otherUser));
        given(sellingBidRepository.findById(any(UUID.class))).willReturn(Optional.of(sellingBid));

        // when & then
        assertThatThrownBy(() -> sellingBidService.updateSellingBidPrice(bidId, 160000, email))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}