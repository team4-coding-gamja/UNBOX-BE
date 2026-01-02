package com.example.unbox_be.domain.wishlist.controller;

import com.example.unbox_be.domain.user.entity.User;
import com.example.unbox_be.domain.wishlist.dto.request.WishlistRequestDTO;
import com.example.unbox_be.domain.wishlist.dto.response.WishlistResponseDTO;
import com.example.unbox_be.domain.wishlist.service.WishlistService;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 1. 위시리스트 추가 (찜하기)
    @PostMapping
    public ResponseEntity<Void> addWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 로그인된 유저 정보 자동 주입
            @RequestBody WishlistRequestDTO requestDTO
    ) {
        wishlistService.addWishlist(userDetails.getUsername(), requestDTO.getOptionId());
        return ResponseEntity.ok().build();
    }

    // 2. 내 위시리스트 목록 조회
    @GetMapping
    public ResponseEntity<List<WishlistResponseDTO>> getMyWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(hidden = true) @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        List<WishlistResponseDTO> response = wishlistService.getMyWishlist(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    // 3. 위시리스트 삭제 (선택 사항)
    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<Void> removeWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID wishlistId
    ) {
        wishlistService.removeWishlist(userDetails.getUsername(), wishlistId);
        return ResponseEntity.noContent().build();
    }
}