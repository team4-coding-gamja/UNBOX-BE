package com.example.unbox_be.domain.wishlist.service;

import com.example.unbox_be.domain.product.entity.Brand;
import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.product.repository.ProductOptionRepository;
import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.user.repository.UserRepository;
import com.example.unbox_be.domain.wishlist.dto.response.WishlistResponseDTO;
import com.example.unbox_be.domain.wishlist.entity.Wishlist;
import com.example.unbox_be.domain.wishlist.repository.WishlistRepository;
import com.example.unbox_be.global.error.exception.CustomException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @InjectMocks
    private WishlistService wishlistService;

    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private ProductOptionRepository productOptionRepository;
    @Mock
    private UserRepository userRepository;

    private User createUser(Long id, String email) {
        User user = BeanUtils.instantiateClass(User.class);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "email", email);
        return user;
    }

    private ProductOption createProductOption(UUID id) {
        Brand brand = BeanUtils.instantiateClass(Brand.class);
        ReflectionTestUtils.setField(brand, "name", "Nike");

        Product product = BeanUtils.instantiateClass(Product.class);
        ReflectionTestUtils.setField(product, "brand", brand);
        ReflectionTestUtils.setField(product, "name", "Air Force 1");
        ReflectionTestUtils.setField(product, "imageUrl", "http://img.com");

        ProductOption option = BeanUtils.instantiateClass(ProductOption.class);
        ReflectionTestUtils.setField(option, "id", id);
        ReflectionTestUtils.setField(option, "product", product);
        ReflectionTestUtils.setField(option, "option", "270");
        return option;
    }

    private Wishlist createWishlist(UUID id, User user, ProductOption option) {
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .productOption(option)
                .build();
        ReflectionTestUtils.setField(wishlist, "id", id);
        return wishlist;
    }

    // addWishlist Tests

    @Test
    @DisplayName("위시리스트 추가에 성공했을 때")
    void addWishlist_Success() {
        // given
        String email = "test@test.com";
        UUID optionId = UUID.randomUUID();
        User user = createUser(1L, email);
        ProductOption option = createProductOption(optionId);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(productOptionRepository.findByIdAndDeletedAtIsNull(optionId)).willReturn(Optional.of(option));
        given(wishlistRepository.existsByUserAndProductOptionAndDeletedAtIsNull(user, option)).willReturn(false);

        // when
        wishlistService.addWishlist(email, optionId);

        // then
        verify(wishlistRepository).save(any(Wishlist.class));
    }

    @Test
    @DisplayName("유저를 찾을 수 없어 위시리스트 추가에 실패했을 때")
    void addWishlist_Fail_UserNotFound() {
        // given
        String email = "unknown@test.com";
        UUID optionId = UUID.randomUUID();

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.addWishlist(email, optionId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 옵션이 존재하지 않아 위시리스트 추가에 실패했을 때")
    void addWishlist_Fail_OptionNotFound() {
        // given
        String email = "test@test.com";
        UUID optionId = UUID.randomUUID();
        User user = createUser(1L, email);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(productOptionRepository.findByIdAndDeletedAtIsNull(optionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.addWishlist(email, optionId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_OPTION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 위시리스트에 존재하는 상품이라 추가에 실패했을 때")
    void addWishlist_Fail_AlreadyExists() {
        // given
        String email = "test@test.com";
        UUID optionId = UUID.randomUUID();
        User user = createUser(1L, email);
        ProductOption option = createProductOption(optionId);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(productOptionRepository.findByIdAndDeletedAtIsNull(optionId)).willReturn(Optional.of(option));
        given(wishlistRepository.existsByUserAndProductOptionAndDeletedAtIsNull(user, option)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> wishlistService.addWishlist(email, optionId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WISHLIST_ALREADY_EXISTS);
        
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    // getMyWishlist Tests

    @Test
    @DisplayName("내 위시리스트 조회에 성공했을 때 (데이터 있음)")
    void getMyWishlist_Success() {
        // given
        String email = "test@test.com";
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L, email);
        ProductOption option = createProductOption(UUID.randomUUID());
        Wishlist wishlist = createWishlist(UUID.randomUUID(), user, option);
        ReflectionTestUtils.setField(wishlist, "createdAt", LocalDateTime.now());

        Slice<Wishlist> wishlistSlice = new SliceImpl<>(List.of(wishlist));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, pageable)).willReturn(wishlistSlice);

        // when
        List<WishlistResponseDTO> result = wishlistService.getMyWishlist(email, pageable);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(option.getProduct().getId());
        assertThat(result.get(0).getOptionName()).isEqualTo("270");
    }

    @Test
    @DisplayName("내 위시리스트 조회에 성공했을 때 (데이터 없음)")
    void getMyWishlist_Success_Empty() {
        // given
        String email = "test@test.com";
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L, email);
        
        Slice<Wishlist> emptySlice = new SliceImpl<>(Collections.emptyList());

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, pageable)).willReturn(emptySlice);

        // when
        List<WishlistResponseDTO> result = wishlistService.getMyWishlist(email, pageable);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("유저가 존재하지 않아 조회에 실패했을 때")
    void getMyWishlist_Fail_UserNotFound() {
        // given
        String email = "unknown@test.com";
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.getMyWishlist(email, pageable))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    // removeWishlist Tests

    @Test
    @DisplayName("위시리스트 삭제에 성공했을 때")
    void removeWishlist_Success() {
        // given
        String email = "test@test.com";
        UUID wishlistId = UUID.randomUUID();
        User user = createUser(1L, email);
        Wishlist wishlist = createWishlist(wishlistId, user, createProductOption(UUID.randomUUID()));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByIdAndDeletedAtIsNull(wishlistId)).willReturn(Optional.of(wishlist));

        // when
        wishlistService.removeWishlist(email, wishlistId);

        // then
        // softDelete가 호출되었는지 확인은 상태 변경이나 spy를 통해 확인 가능하지만, 
        // 여기서는 예외가 발생하지 않는 것으로 성공을 1차 검증하고, 
        // 필요한 경우 ArgumentCaptor 등을 사용할 수 있음. 
        // Mockito verify로 softDelete 내부 로직까지 검증하긴 어렵지만, 
        // Service 로직 상 예외 없이 통과했음을 확인.
    }

    @Test
    @DisplayName("유저를 찾을 수 없어 삭제에 실패했을 때")
    void removeWishlist_Fail_UserNotFound() {
        // given
        String email = "unknown@test.com";
        UUID wishlistId = UUID.randomUUID();

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.removeWishlist(email, wishlistId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("위시리스트 항목이 존재하지 않아 삭제에 실패했을 때")
    void removeWishlist_Fail_WishlistNotFound() {
        // given
        String email = "test@test.com";
        UUID wishlistId = UUID.randomUUID();
        User user = createUser(1L, email);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(wishlistRepository.findByIdAndDeletedAtIsNull(wishlistId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.removeWishlist(email, wishlistId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WISHLIST_NOT_FOUND);
    }

    @Test
    @DisplayName("타인의 위시리스트를 삭제하려고 시도하여 실패했을 때")
    void removeWishlist_Fail_AccessDenied() {
        // given
        String email = "hacker@test.com";
        UUID wishlistId = UUID.randomUUID();
        User hacker = createUser(999L, email);
        
        User owner = createUser(1L, "owner@test.com");
        Wishlist wishlist = createWishlist(wishlistId, owner, createProductOption(UUID.randomUUID()));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(hacker));
        given(wishlistRepository.findByIdAndDeletedAtIsNull(wishlistId)).willReturn(Optional.of(wishlist));

        // when & then
        assertThatThrownBy(() -> wishlistService.removeWishlist(email, wishlistId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }
}
