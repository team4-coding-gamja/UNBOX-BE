package com.example.unbox_be.domain.trade.service;

import com.example.unbox_be.domain.order.entity.Order;
import com.example.unbox_be.domain.order.entity.OrderStatus;
import com.example.unbox_be.domain.order.repository.OrderRepository;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.domain.payment.entity.Payment;
import com.example.unbox_be.domain.payment.entity.PaymentMethod;
import com.example.unbox_be.domain.payment.entity.PaymentStatus;
import com.example.unbox_be.domain.payment.repository.PaymentRepository;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.settlement.service.SettlementService;
import com.example.unbox_be.domain.trade.dto.request.SellingBidRequestDto;
import com.example.unbox_be.domain.trade.dto.response.SellingBidResponseDto;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.mapper.SellingBidMapper;
import com.example.unbox_be.domain.trade.repository.SellingBidRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SellingBidServiceTest {

    @InjectMocks
    private SellingBidService sellingBidService;

    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private SellingBidRepository sellingBidRepository;
    @Mock private SellingBidMapper sellingBidMapper;

    private final Long userId = 1L;
    private final UUID optionId = UUID.randomUUID();
    private final UUID bidId = UUID.randomUUID();

    /**
     * 리플렉션 유틸리티: 엔티티 생성 및 ID 주입
     */
    private <T> T createMockEntity(Class<T> clazz, Object id) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T entity = constructor.newInstance();
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    @Nested
    @DisplayName("판매 입찰 생성 테스트 (createSellingBid)")
    class CreateSellingBidTest {

        @Test
        @DisplayName("성공 - 정상적인 데이터로 입찰 생성")
        void createSellingBid_success_normal() throws Exception {
            // given
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(150000))
                    .build();
            ProductOption option = createMockEntity(ProductOption.class, optionId);
            SellingBid bid = createMockEntity(SellingBid.class, bidId);

            doReturn(Optional.of(option)).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);
            doReturn(bid).when(sellingBidMapper).toEntity(eq(requestDto), eq(userId), any(), eq(option));
            doReturn(bid).when(sellingBidRepository).save(any());

            // when
            UUID resultId = sellingBidService.createSellingBid(userId, requestDto);

            // then
            assertEquals(bidId, resultId);
            verify(sellingBidRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 optionId 사용 (PRODUCT_NOT_FOUND)")
        void createSellingBid_fail_optionNotFound() {
            // given
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(150000))
                    .build();
            doReturn(Optional.empty()).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.createSellingBid(userId, requestDto));
            assertEquals(ErrorCode.PRODUCT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("실패 - 삭제된(deletedAt != null) 옵션 사용")
        void createSellingBid_fail_deletedOption() {
            // given
            // findByIdAndDeletedAtIsNull 쿼리 자체가 null을 반환하도록 설정
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(150000))
                    .build();
            doReturn(Optional.empty()).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);

            // when & then
            assertThrows(CustomException.class, () -> sellingBidService.createSellingBid(userId, requestDto));
        }

        @Test
        @DisplayName("성공 - 마감 기한이 정확히 오늘로부터 30일 뒤 00시인지 확인")
        void createSellingBid_success_verifyDeadline() throws Exception {
            // given
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(150000))
                    .build();
            ProductOption option = createMockEntity(ProductOption.class, optionId);
            SellingBid bid = createMockEntity(SellingBid.class, bidId);

            doReturn(Optional.of(option)).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);

            // ArgumentCaptor를 사용하여 전달되는 deadline 값 캡처
            ArgumentCaptor<LocalDateTime> deadlineCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            given(sellingBidMapper.toEntity(eq(requestDto), eq(userId), deadlineCaptor.capture(), eq(option)))
                    .willReturn(bid);
            given(sellingBidRepository.save(any())).willReturn(bid);

            // when
            sellingBidService.createSellingBid(userId, requestDto);

            // then
            LocalDateTime expectedDeadline = LocalDate.now().plusDays(30).atStartOfDay();
            assertEquals(expectedDeadline, deadlineCaptor.getValue(), "마감 기한은 오늘+30일 00:00여야 함");
        }

        @Test
        @DisplayName("실패 - 입찰 가격이 음수이거나 0원인 경우 (서비스 로직 혹은 DTO 검증)")
        void createSellingBid_fail_invalidPrice() throws Exception {
            // given
            // 빌더 패턴을 사용하여 0원인 요청 생성
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(0))
                    .build();

            // 🔴 [삭제] 이 부분은 서비스 로직 상단에서 가격 체크에 걸려 실행되지 않으므로 삭제해야 합니다.
            // doReturn(Optional.of(option)).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);

            // when & then
            CustomException ex = assertThrows(CustomException.class, () ->
                    sellingBidService.createSellingBid(userId, requestDto));

            assertEquals(ErrorCode.INVALID_BID_PRICE, ex.getErrorCode());
        }

        @Test
        @DisplayName("6. 성공 - Mapper가 DTO를 엔티티로 정확히 변환하는지 확인")
        void createSellingBid_success_mapperMapping() throws Exception {
            // given
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(200000))
                    .build();
            ProductOption option = createMockEntity(ProductOption.class, optionId);
            SellingBid bid = createMockEntity(SellingBid.class, bidId);

            doReturn(Optional.of(option)).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);

            // then 연동
            given(sellingBidMapper.toEntity(eq(requestDto), eq(userId), any(), eq(option))).willReturn(bid);
            given(sellingBidRepository.save(bid)).willReturn(bid);

            // when
            sellingBidService.createSellingBid(userId, requestDto);

            // then
            verify(sellingBidMapper).toEntity(eq(requestDto), eq(userId), any(), eq(option));
        }

        @Test
        @DisplayName("7. 성공 - 저장된 후 생성된 UUID가 정상 반환되는지 확인")
        void createSellingBid_success_returnUuid() throws Exception {
            // given
            SellingBidRequestDto requestDto = SellingBidRequestDto.builder()
                    .optionId(optionId)
                    .price(BigDecimal.valueOf(100000))
                    .build();
            ProductOption option = createMockEntity(ProductOption.class, optionId);

            // 각각 다른 ID를 가진 입찰 객체 생성
            UUID expectedId = UUID.randomUUID();
            SellingBid savedBid = createMockEntity(SellingBid.class, expectedId);

            doReturn(Optional.of(option)).when(productOptionRepository).findByIdAndDeletedAtIsNull(optionId);
            doReturn(savedBid).when(sellingBidMapper).toEntity(any(), any(), any(), any());
            doReturn(savedBid).when(sellingBidRepository).save(any());

            // when
            UUID resultId = sellingBidService.createSellingBid(userId, requestDto);

            // then
            assertEquals(expectedId, resultId, "반환된 ID는 저장된 엔티티의 ID와 일치해야 함");
        }
    }
    @Nested
    @DisplayName("판매 입찰 취소 테스트 (cancelSellingBid)")
    class CancelSellingBidTest {

        @Test
        @DisplayName("1. 성공 - LIVE 상태인 본인의 입찰을 정상적으로 취소한다")
        void cancelSellingBid_success_normal() throws Exception {
            // given
            // Spy를 사용하여 실제 엔티티의 상태 변화를 추적합니다.
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.cancelSellingBid(bidId, userId, "user@test.com");

            // then
            assertEquals(SellingStatus.CANCELLED, bid.getStatus(), "상태가 CANCELLED로 변경되어야 함");
            verify(bid, times(1)).updateStatus(SellingStatus.CANCELLED);
        }

        @Test
        @DisplayName("2. 실패 - 존재하지 않는 입찰 ID 조회 (BID_NOT_FOUND)")
        void cancelSellingBid_fail_notFound() {
            // given
            doReturn(Optional.empty()).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.cancelSellingBid(bidId, userId, "user@test.com"));
            assertEquals(ErrorCode.BID_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("3. 실패 - 타인의 입찰을 취소 시도 (ACCESS_DENIED)")
        void cancelSellingBid_fail_accessDenied() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", 999L); // 다른 유저 ID 설정

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.cancelSellingBid(bidId, userId, "user@test.com"));
            assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        }

        @Test
        @DisplayName("4. 실패 - 이미 체결된(MATCHED) 입찰은 취소할 수 없다")
        void cancelSellingBid_fail_invalidStatusMatched() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.MATCHED); // 이미 매칭된 상태

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.cancelSellingBid(bidId, userId, "user@test.com"));
            assertEquals(ErrorCode.INVALID_ORDER_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("5. 실패 - 이미 취소된(CANCELLED) 입찰을 중복 취소 시도")
        void cancelSellingBid_fail_alreadyCancelled() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.CANCELLED);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.cancelSellingBid(bidId, userId, "user@test.com"));
            assertEquals(ErrorCode.INVALID_ORDER_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("6. 성공 - 취소 시 수정자(email) 정보가 엔티티에 기록되는지 확인")
        void cancelSellingBid_success_checkModifiedBy() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            String modifierEmail = "admin@unbox.com";

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.cancelSellingBid(bidId, userId, modifierEmail);

            // then
            verify(bid).updateModifiedBy(modifierEmail);
        }

        @Test
        @DisplayName("7. 실패 - 입찰 ID가 null인 경우 조회 실패 처리")
        void cancelSellingBid_fail_idIsNull() {
            // given
            doReturn(Optional.empty()).when(sellingBidRepository).findByIdAndDeletedAtIsNull(null);

            // when & then
            assertThrows(CustomException.class,
                    () -> sellingBidService.cancelSellingBid(null, userId, "user@test.com"));
        }
    }
    @Nested
    @DisplayName("판매 입찰 가격 수정 테스트 (updateSellingBidPrice)")
    class UpdateSellingBidPriceTest {

        @Test
        @DisplayName("1. 성공 - 모든 조건 충족 시 가격이 정상적으로 수정된다")
        void updatePrice_success_normal() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            BigDecimal newPrice = BigDecimal.valueOf(200000);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.updateSellingBidPrice(bidId, newPrice, userId, "user@test.com");

            // then
            verify(bid, times(1)).updatePrice(eq(newPrice), eq(userId), anyString());
        }

        @Test
        @DisplayName("2. 실패 - 존재하지 않는 입찰 ID 수정 시도 (BID_NOT_FOUND)")
        void updatePrice_fail_notFound() {
            // given
            doReturn(Optional.empty()).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.updateSellingBidPrice(bidId, BigDecimal.valueOf(200000), userId, "user@test.com"));
            assertEquals(ErrorCode.BID_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("3. 실패 - 타인의 입찰 가격을 수정하려 할 때 (ACCESS_DENIED)")
        void updatePrice_fail_accessDenied() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", 999L); // 다른 유저

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.updateSellingBidPrice(bidId, BigDecimal.valueOf(200000), userId, "user@test.com"));
            assertEquals(ErrorCode.ACCESS_DENIED, ex.getErrorCode());
        }

        @Test
        @DisplayName("4. 실패 - 변경하려는 가격이 0원일 때 (INVALID_BID_PRICE)")
        void updatePrice_fail_priceIsZero() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.updateSellingBidPrice(bidId, BigDecimal.valueOf(0), userId, "user@test.com"));
            assertEquals(ErrorCode.INVALID_BID_PRICE, ex.getErrorCode());
        }

        @Test
        @DisplayName("5. 실패 - 변경하려는 가격이 null일 때 (INVALID_BID_PRICE)")
        void updatePrice_fail_priceIsNull() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.updateSellingBidPrice(bidId, null, userId, "user@test.com"));
            assertEquals(ErrorCode.INVALID_BID_PRICE, ex.getErrorCode());
        }

        @Test
        @DisplayName("6. 실패 - LIVE 상태가 아닌 입찰(예: MATCHED)은 가격 수정 불가")
        void updatePrice_fail_invalidStatus() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.MATCHED);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.updateSellingBidPrice(bidId, BigDecimal.valueOf(200000), userId, "user@test.com"));
            assertEquals(ErrorCode.INVALID_ORDER_STATUS, ex.getErrorCode());
        }

        @Test
        @DisplayName("7. 성공 - 수정 후 가격 정보가 엔티티에 반영되었는지 확인")
        void updatePrice_success_checkData() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            ReflectionTestUtils.setField(bid, "price", BigDecimal.valueOf(100000));

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.updateSellingBidPrice(bidId, BigDecimal.valueOf(300000), userId, "user@test.com");

            // then
            // 엔티티의 updatePrice가 내부 필드를 바꾸는지 리플렉션으로 검증
            assertEquals(BigDecimal.valueOf(300000), ReflectionTestUtils.getField(bid, "price"));
        }
    }

    @Nested
    @DisplayName("판매 입찰 상세 조회 테스트 (getSellingBidDetail)")
    class GetSellingBidDetailTest {

        @Test
        @DisplayName("1. 성공 - 본인의 입찰 정보를 상품/옵션 정보와 함께 반환한다")
        void getDetail_success() throws Exception {
            // given
            Product product = createMockEntity(Product.class, UUID.randomUUID());
            ReflectionTestUtils.setField(product, "name", "테스트 신발");

            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", product);
            ReflectionTestUtils.setField(option, "option", "270");

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            SellingBidResponseDto mockDto = SellingBidResponseDto.builder().id(bidId).build();

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            doReturn(mockDto).when(sellingBidMapper).toResponseDto(bid);

            // when
            SellingBidResponseDto result = sellingBidService.getSellingBidDetail(bidId, userId);

            // then
            assertNotNull(result);
            assertEquals("테스트 신발", result.getProduct().getName());
            assertEquals("270", result.getSize());
        }

        @Test
        @DisplayName("2. 실패 - 존재하지 않는 입찰 ID 조회 (BID_NOT_FOUND)")
        void getDetail_fail_notFound() {
            doReturn(Optional.empty()).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            assertThrows(CustomException.class, () -> sellingBidService.getSellingBidDetail(bidId, userId));
        }

        @Test
        @DisplayName("3. 실패 - 타인의 입찰 정보를 조회 시도 (ACCESS_DENIED)")
        void getDetail_fail_accessDenied() throws Exception {
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", 999L); // 다른 유저

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            assertThrows(CustomException.class, () -> sellingBidService.getSellingBidDetail(bidId, userId));
        }

        @Test
        @DisplayName("4. 성공 - 삭제되지 않은(deletedAt is null) 데이터만 조회되는지 확인")
        void getDetail_success_checkFilter() throws Exception {
            //given
            Product product = createMockEntity(Product.class, UUID.randomUUID());

            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", product); // 이 부분이 누락되었었습니다.

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            doReturn(SellingBidResponseDto.builder().build()).when(sellingBidMapper).toResponseDto(bid);

            // when
            sellingBidService.getSellingBidDetail(bidId, userId);

            // then
            verify(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
        }

        @Test
        @DisplayName("5. 성공 - 상품 이미지가 올바르게 응답에 포함되는지 확인")
        void getDetail_success_productImage() throws Exception {
            Product product = createMockEntity(Product.class, UUID.randomUUID());
            ReflectionTestUtils.setField(product, "imageUrl", "http://image.com");
            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", product);

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            doReturn(SellingBidResponseDto.builder().build()).when(sellingBidMapper).toResponseDto(bid);

            SellingBidResponseDto result = sellingBidService.getSellingBidDetail(bidId, userId);

            assertEquals("http://image.com", result.getProduct().getImageUrl());
        }

        @Test
        @DisplayName("6. 성공 - 매퍼를 통해 반환된 기본 DTO 정보(가격, 상태)가 유지되는지 확인")
        void getDetail_success_dtoMaintain() throws Exception {
            Product product = createMockEntity(Product.class, UUID.randomUUID());

            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", product); // 이 부분이 누락되었었습니다.

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            SellingBidResponseDto mockDto = SellingBidResponseDto.builder()
                    .price(150000)
                    .status(SellingStatus.LIVE)
                    .build();

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            doReturn(mockDto).when(sellingBidMapper).toResponseDto(bid);

            // when
            SellingBidResponseDto result = sellingBidService.getSellingBidDetail(bidId, userId);

            // then
            assertEquals(150000, result.getPrice());
            assertEquals(SellingStatus.LIVE, result.getStatus());
        }

        @Test
        @DisplayName("7. 실패 - 입찰에 연관된 상품 옵션이 없는 경우 (예외 상황)")
        void getDetail_fail_noOption() throws Exception {
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", null); // 옵션 없음

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.getSellingBidDetail(bidId, userId));

            assertEquals(ErrorCode.INVALID_BID_STATUS, ex.getErrorCode());
        }
        @Test
        @DisplayName("상세 조회 실패 - 옵션은 존재하나 연관된 상품 정보가 없는 경우")
        void getDetail_fail_productIsNullInOption() throws Exception {
            // given
            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", null); // 🚩 의도적인 데이터 결함

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class,
                    () -> sellingBidService.getSellingBidDetail(bidId, userId));
            assertEquals(ErrorCode.INVALID_BID_STATUS, ex.getErrorCode());
        }
        @Test
        @DisplayName("상태 변경 성공 - 이메일이 null로 전달되어도 예외 없이 진행된다")
        void cancelBid_success_emailIsNull() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "userId", userId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);

            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            assertDoesNotThrow(() -> sellingBidService.cancelSellingBid(bidId, userId, null));

            // then
            verify(bid, never()).updateModifiedBy(any()); // 🚩 이메일이 없으므로 호출 안 됨을 확인
        }

    @Nested
    @DisplayName("시스템용 상태 변경 테스트 (updateSellingBidStatusBySystem)")
    class UpdateStatusBySystemTest {

        @Test
        @DisplayName("1. 성공 - 시스템 호출 시 권한 확인 없이 LIVE에서 MATCHED로 상태를 변경한다")
        void updateStatusBySystem_success_matched() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);

            // Repository는 해당 입찰을 반환하도록 설정
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.MATCHED, "SYSTEM_ADMIN");

            // then
            assertEquals(SellingStatus.MATCHED, bid.getStatus());
            verify(bid, times(1)).updateStatus(SellingStatus.MATCHED);
        }

        @Test
        @DisplayName("2. 성공 - 시스템 호출 시 LIVE에서 CANCELLED로 상태를 변경한다")
        void updateStatusBySystem_success_cancelled() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.CANCELLED, "SYSTEM_BATCH");

            // then
            assertEquals(SellingStatus.CANCELLED, bid.getStatus());
        }

        @Test
        @DisplayName("3. 실패 - 존재하지 않는 입찰 ID로 상태 변경 시도 (BID_NOT_FOUND)")
        void updateStatusBySystem_fail_notFound() {
            // given
            doReturn(Optional.empty()).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class, () ->
                    sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.MATCHED, "SYSTEM"));
            assertEquals(ErrorCode.BID_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("4. 실패 - 이미 MATCHED(체결)된 입찰을 다시 LIVE로 되돌릴 수 없다 (전이 규칙 위반)")
        void updateStatusBySystem_fail_invalidTransition_matchedToLive() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.MATCHED);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            CustomException ex = assertThrows(CustomException.class, () ->
                    sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.LIVE, "SYSTEM"));
            assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
        }

        @Test
        @DisplayName("5. 실패 - CANCELLED(취소)된 입찰을 LIVE로 되돌릴 수 없다")
        void updateStatusBySystem_fail_invalidTransition_cancelledToLive() throws Exception {
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "status", SellingStatus.CANCELLED);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            assertThrows(CustomException.class, () ->
                    sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.LIVE, "SYSTEM"));
        }

        @Test
        @DisplayName("6. 성공 - 상태 변경 시 전달받은 email이 수정자로 기록된다")
        void updateStatusBySystem_success_checkModifiedBy() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);
            String systemEmail = "admin@unbox.com";

            // when
            sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.MATCHED, systemEmail);

            // then
            verify(bid).updateModifiedBy(systemEmail);
        }

        @Test
        @DisplayName("7. 성공 - 동일한 상태로의 변경 시도 시 전이 규칙 통과 여부 확인 (Idempotency)")
        void updateStatusBySystem_success_sameStatus() throws Exception {
            // given
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            assertDoesNotThrow(() ->
                    sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.LIVE, "SYSTEM"));
        }
        @Test
        @DisplayName("가격 수정 실패 - 가격이 null인 경우")
        void updatePrice_fail_priceIsNull_explicit() throws Exception{
            // given
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "userId", userId);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when & then
            assertThrows(CustomException.class, () ->
                    sellingBidService.updateSellingBidPrice(bidId, null, userId, "email"));
        }
        @Test
        @DisplayName("상태 변경 실패 - 유저 ID가 없는 경우 Access Denied")
        void updateStatus_fail_userIdNull() {
            assertThrows(CustomException.class, () ->
                    sellingBidService.updateSellingBidStatus(bidId, SellingStatus.CANCELLED, null, "email"));
        }
        @Test
        @DisplayName("시스템 상태 변경 - 이메일이 null인 경우 수정자 기록을 건너뛴다")
        void updateStatusBySystem_success_emailNull() throws Exception {
            SellingBid bid = spy(createMockEntity(SellingBid.class, bidId));
            ReflectionTestUtils.setField(bid, "status", SellingStatus.LIVE);
            doReturn(Optional.of(bid)).when(sellingBidRepository).findByIdAndDeletedAtIsNull(bidId);

            // when
            sellingBidService.updateSellingBidStatusBySystem(bidId, SellingStatus.MATCHED, null);

            // then
            verify(bid, never()).updateModifiedBy(anyString());
        }
    }

    @Nested
    @DisplayName("내 판매 입찰 목록 조회 테스트 (getMySellingBids)")
    class GetMySellingBidsTest {

        @Test
        @DisplayName("1. 성공 - 페이징 처리된 내 입찰 목록을 정상적으로 반환한다")
        void getMyBids_success() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", createMockEntity(Product.class, UUID.randomUUID()));
            ReflectionTestUtils.setField(option, "option", "260");

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            Slice<SellingBid> bidSlice = new SliceImpl<>(java.util.List.of(bid), pageable, false);

            given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).willReturn(bidSlice);
            given(sellingBidMapper.toResponseDto(any())).willReturn(SellingBidResponseDto.builder().build());

            // when
            Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(userId, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            verify(sellingBidRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        @Test
        @DisplayName("2. 성공 - 입찰 목록이 비어있을 때 빈 슬라이스를 반환한다")
        void getMyBids_success_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(new SliceImpl<>(java.util.List.of()));

            Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(userId, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("3. 성공 - 상품 옵션이 없는 데이터가 섞여있어도 NPE 없이 처리된다")
        void getMyBids_success_nullOptionHandling() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "productOption", null); // 옵션 없음

            given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(new SliceImpl<>(java.util.List.of(bid)));
            given(sellingBidMapper.toResponseDto(any())).willReturn(SellingBidResponseDto.builder().build());

            Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(userId, pageable);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("4. 성공 - 다음 페이지 존재 여부(hasNext)가 정상적으로 전달된다")
        void getMyBids_success_hasNext() throws Exception { // throws Exception 추가
            Pageable pageable = PageRequest.of(0, 1);

            // 에러 발생 지점: new SellingBid() 대신 createMockEntity 사용
            SellingBid bid = createMockEntity(SellingBid.class, UUID.randomUUID());

            Slice<SellingBid> hasNextSlice = new SliceImpl<>(java.util.List.of(bid), pageable, true);

            given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).willReturn(hasNextSlice);
            given(sellingBidMapper.toResponseDto(any())).willReturn(SellingBidResponseDto.builder().build());

            Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(userId, pageable);

            assertThat(result.hasNext()).isTrue();
        }

        @Test
        @DisplayName("5. 성공 - Mapper를 통해 변환된 DTO에 상품 정보가 정확히 매핑되는지 확인")
        void getMyBids_success_mappingCheck() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Product product = createMockEntity(Product.class, UUID.randomUUID());
            ReflectionTestUtils.setField(product, "name", "Nike Jordan");

            ProductOption option = createMockEntity(ProductOption.class, UUID.randomUUID());
            ReflectionTestUtils.setField(option, "product", product);
            ReflectionTestUtils.setField(option, "option", "280");

            SellingBid bid = createMockEntity(SellingBid.class, bidId);
            ReflectionTestUtils.setField(bid, "productOption", option);

            given(sellingBidRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(new SliceImpl<>(java.util.List.of(bid)));
            given(sellingBidMapper.toResponseDto(any())).willReturn(SellingBidResponseDto.builder().build());

            Slice<SellingBidResponseDto> result = sellingBidService.getMySellingBids(userId, pageable);

            assertThat(result.getContent().get(0).getProduct().getName()).isEqualTo("Nike Jordan");
            assertThat(result.getContent().get(0).getSize()).isEqualTo("280");
        }
    }
}
}
