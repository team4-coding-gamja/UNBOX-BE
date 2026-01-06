package com.example.unbox_be.domain.cart.service;

import com.example.unbox_be.domain.cart.dto.request.CartRequestDto;
import com.example.unbox_be.domain.cart.dto.response.CartResponseDto;
import com.example.unbox_be.domain.cart.entity.Cart;
import com.example.unbox_be.domain.cart.repository.CartRepository;
import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.trade.entity.SellingBid;
import com.example.unbox_be.domain.trade.entity.SellingStatus;
import com.example.unbox_be.domain.trade.service.SellingBidService;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.service.UserService;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

   @InjectMocks
   private CartServiceImpl cartService;

   @Mock
   private CartRepository cartRepository;

   @Mock
   private UserService userService;

   @Mock
   private SellingBidService sellingBidService;

   // --- Helper Methods ---
   private User createMockUser(Long id) {
       User user = mock(User.class);
       org.mockito.Mockito.lenient().when(user.getId()).thenReturn(id);
       return user;
   }

   private SellingBid createMockBid(Long userId, SellingStatus status) {
       SellingBid bid = mock(SellingBid.class);
       org.mockito.Mockito.lenient().when(bid.getUserId()).thenReturn(userId);
       org.mockito.Mockito.lenient().when(bid.getStatus()).thenReturn(status);
       return bid;
   }

   // 1. addCart: 성공
   @Test
   @DisplayName("장바구니 담기 성공: LIVE 상태이고 본인 상품이 아니며 중복되지 않음")
   void addCart_Success() {
       Long userId = 1L;
       UUID bidId = UUID.randomUUID();
       CartRequestDto req = new CartRequestDto();
       ReflectionTestUtils.setField(req, "sellingBidId", bidId);

       User user = createMockUser(userId);
       SellingBid bid = createMockBid(2L, SellingStatus.LIVE);

       given(userService.findUser(userId)).willReturn(user);
       given(sellingBidService.findSellingBidById(bidId)).willReturn(bid);
       given(cartRepository.existsByUserAndSellingBid(user, bid)).willReturn(false);

       cartService.addCart(userId, req);

       verify(cartRepository).save(any(Cart.class));
   }

   // 2. addCart: 실패 - 상태가 LIVE가 아님
   @Test
   @DisplayName("장바구니 담기 실패: 상품 상태가 LIVE가 아님 (MATCHED)")
   void addCart_Fail_NotLive() {
       Long userId = 1L;
       UUID bidId = UUID.randomUUID();
       CartRequestDto req = new CartRequestDto();
       ReflectionTestUtils.setField(req, "sellingBidId", bidId);

       User user = createMockUser(userId);
       SellingBid bid = createMockBid(2L, SellingStatus.MATCHED);

       given(userService.findUser(userId)).willReturn(user);
       given(sellingBidService.findSellingBidById(bidId)).willReturn(bid);

       assertThatThrownBy(() -> cartService.addCart(userId, req))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.BID_NOT_AVAILABLE_FOR_CART.getMessage());
   }

   // 3. addCart: 실패 - 본인 상품
   @Test
   @DisplayName("장바구니 담기 실패: 본인이 등록한 상품")
   void addCart_Fail_OwnBid() {
       Long userId = 1L;
       UUID bidId = UUID.randomUUID();
       CartRequestDto req = new CartRequestDto();
       ReflectionTestUtils.setField(req, "sellingBidId", bidId);

       User user = createMockUser(userId);
       SellingBid bid = createMockBid(userId, SellingStatus.LIVE); // Same User ID

       given(userService.findUser(userId)).willReturn(user);
       given(sellingBidService.findSellingBidById(bidId)).willReturn(bid);

       assertThatThrownBy(() -> cartService.addCart(userId, req))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.CANNOT_ADD_MY_OWN_BID.getMessage());
   }

   // 4. addCart: 실패 - 이미 장바구니에 존재
   @Test
   @DisplayName("장바구니 담기 실패: 이미 담겨있는 상품")
   void addCart_Fail_Duplicate() {
       Long userId = 1L;
       UUID bidId = UUID.randomUUID();
       CartRequestDto req = new CartRequestDto();
       ReflectionTestUtils.setField(req, "sellingBidId", bidId);

       User user = createMockUser(userId);
       SellingBid bid = createMockBid(2L, SellingStatus.LIVE);

       given(userService.findUser(userId)).willReturn(user);
       given(sellingBidService.findSellingBidById(bidId)).willReturn(bid);
       given(cartRepository.existsByUserAndSellingBid(user, bid)).willReturn(true);

       assertThatThrownBy(() -> cartService.addCart(userId, req))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.CART_ALREADY_EXISTS.getMessage());
   }

   // 5. getMyCarts: 조회 성공 및 매핑 검증
   @Test
   @DisplayName("장바구니 조회: DTO 매핑 확인")
   void getMyCarts_Success() {
       Long userId = 1L;
       User user = createMockUser(userId);

       // Mock Cart Structure
       Cart cart = mock(Cart.class);
       SellingBid bid = mock(SellingBid.class);
       ProductOption option = mock(ProductOption.class);
       Product product = mock(Product.class);
       Brand brand = mock(Brand.class);

       given(cart.getId()).willReturn(100L);
       given(cart.getCreatedAt()).willReturn(LocalDateTime.now());
       given(cart.getSellingBid()).willReturn(bid);

       given(bid.getId()).willReturn(UUID.randomUUID());
       given(bid.getPrice()).willReturn(150000); // Integer
       given(bid.getStatus()).willReturn(SellingStatus.LIVE);
       given(bid.getProductOption()).willReturn(option);

       given(option.getOption()).willReturn("280");
       given(option.getProduct()).willReturn(product);

        UUID productId = UUID.randomUUID();
        given(product.getId()).willReturn(productId);
        given(product.getName()).willReturn("Test Product");
        given(product.getImageUrl()).willReturn("http://img.com");
        given(product.getBrand()).willReturn(brand);

        given(brand.getName()).willReturn("Nike");

        given(userService.findUser(userId)).willReturn(user);
        given(cartRepository.findAllByUserOrderByCreatedAtDesc(user)).willReturn(List.of(cart));

        List<CartResponseDto> result = cartService.getMyCarts(userId);

        assertThat(result).hasSize(1);
        CartResponseDto dto = result.get(0);
        assertThat(dto.getCartId()).isEqualTo(100L);
        assertThat(dto.getSellingBid().getProduct().getName()).isEqualTo("Test Product");
        assertThat(dto.getSellingBid().getProduct().getBrandName()).isEqualTo("Nike");
        // Verify Product ID
        assertThat(dto.getSellingBid().getProduct().getId()).isEqualTo(productId);
   }

   // 6. getMyCarts: 빈 목록
   @Test
   @DisplayName("장바구니 조회: 목록이 비었을 때")
   void getMyCarts_Empty() {
       Long userId = 1L;
       User user = createMockUser(userId);
       given(userService.findUser(userId)).willReturn(user);
       given(cartRepository.findAllByUserOrderByCreatedAtDesc(user)).willReturn(Collections.emptyList());

       List<CartResponseDto> result = cartService.getMyCarts(userId);
       assertThat(result).isEmpty();
   }

   // 7. deleteCart: 성공
   @Test
   @DisplayName("장바구니 삭제: 본인 소유 정상 삭제")
   void deleteCart_Success() {
       Long userId = 1L;
       Long cartId = 100L;

       Cart cart = mock(Cart.class);
       User owner = mock(User.class);
       given(cart.getUser()).willReturn(owner);
       given(owner.getId()).willReturn(userId);
       given(owner.getEmail()).willReturn("test@email.com");

       given(cartRepository.findByIdAndDeletedAtIsNull(cartId)).willReturn(Optional.of(cart));

       cartService.deleteCart(userId, cartId);

       verify(cart).softDelete("test@email.com");
   }

   // 8. deleteCart: 실패 - 존재하지 않음
   @Test
   @DisplayName("장바구니 삭제 실패: 존재하지 않거나 이미 삭제됨")
   void deleteCart_Fail_NotFound() {
       Long userId = 1L;
       Long cartId = 999L;

       given(cartRepository.findByIdAndDeletedAtIsNull(cartId)).willReturn(Optional.empty());

       assertThatThrownBy(() -> cartService.deleteCart(userId, cartId))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
   }

   // 9. deleteCart: 실패 - 소유자 불일치
   @Test
   @DisplayName("장바구니 삭제 실패: 본인의 장바구니가 아님")
   void deleteCart_Fail_NotOwner() {
       Long userId = 1L;
       Long otherUserId = 2L;
       Long cartId = 100L;

       Cart cart = mock(Cart.class);
       User owner = mock(User.class);
       given(cart.getUser()).willReturn(owner);
       given(owner.getId()).willReturn(otherUserId); // Different ID

       given(cartRepository.findByIdAndDeletedAtIsNull(cartId)).willReturn(Optional.of(cart));

       assertThatThrownBy(() -> cartService.deleteCart(userId, cartId))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.NOT_CART_OWNER.getMessage());
   }

   // 10. deleteAllCarts: 성공
   @Test
   @DisplayName("장바구니 전체 삭제: 모든 항목에 대해 Soft Delete 호출 확인")
   void deleteAllCarts_Success() {
       Long userId = 1L;
       User user = createMockUser(userId);
       given(user.getEmail()).willReturn("user@email.com");
       given(userService.findUser(userId)).willReturn(user);

       Cart cart1 = mock(Cart.class);
       Cart cart2 = mock(Cart.class);
       given(cartRepository.findAllByUser(user)).willReturn(List.of(cart1, cart2));

       cartService.deleteAllCarts(userId);

       verify(cart1).softDelete("user@email.com");
       verify(cart2).softDelete("user@email.com");
   }

   // 11. deleteAllCarts: 빈 목록일 때
   @Test
   @DisplayName("장바구니 전체 삭제: 삭제할 항목이 없을 때도 에러 없이 통과")
   void deleteAllCarts_Empty() {
       Long userId = 1L;
       User user = createMockUser(userId);
       given(userService.findUser(userId)).willReturn(user);
       given(cartRepository.findAllByUser(user)).willReturn(Collections.emptyList());

       cartService.deleteAllCarts(userId);

       // No exception
   }

   // 12. Exception Propagate: UserService
   @Test
   @DisplayName("장바구니 담기: 유저 조회 실패 시 예외 전파")
   void addCart_UserNotFound() {
       Long userId = 99L;
       CartRequestDto req = new CartRequestDto();
       given(userService.findUser(userId)).willThrow(new CustomException(ErrorCode.USER_NOT_FOUND));

       assertThatThrownBy(() -> cartService.addCart(userId, req))
               .isInstanceOf(CustomException.class)
               .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
   }
}
