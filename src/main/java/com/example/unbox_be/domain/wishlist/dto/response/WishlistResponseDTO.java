package com.example.unbox_be.domain.wishlist.dto.response;

import com.example.unbox_be.domain.product.entity.Product;
import com.example.unbox_be.domain.product.entity.ProductOption;
import com.example.unbox_be.domain.wishlist.entity.Wishlist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class WishlistResponseDTO {
    private UUID wishlistId;     // 위시리스트 항목 고유 ID
    private UUID productId;      // 상품 ID
    private String productName;  // 상품명
    private String optionName;   // 옵션명 (예: 255, Black 등)
    private String imageUrl;     // 상품 이미지 (필요시)

    // Entity를 DTO로 변환하는 정적 팩토리 메서드
    public static WishlistResponseDTO from(Wishlist wishlist) {
        ProductOption option = wishlist.getProductOption();
        Product product = option.getProduct();

        return WishlistResponseDTO.builder()
                .wishlistId(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .optionName(option.getOption())
                .imageUrl(product.getProductImageUrl()) // 이미지 필드가 있다면 추가
                .build();
    }
}