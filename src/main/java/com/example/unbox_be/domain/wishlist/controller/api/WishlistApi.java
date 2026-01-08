package com.example.unbox_be.domain.wishlist.controller.api;

import com.example.unbox_be.domain.wishlist.dto.request.WishlistRequestDTO;
import com.example.unbox_be.domain.wishlist.dto.response.WishlistResponseDTO;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "위시리스트 관리", description = "위시리스트(찜) 관리 API")
@RequestMapping("/api/wishlist")
public interface WishlistApi {

    // 1) 위시리스트 추가 (찜하기)
    @Operation(
            summary = "위시리스트 추가",
            description = "로그인한 사용자가 옵션(optionId)을 위시리스트에 추가(찜)합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위시리스트 추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(옵션 ID 누락/유효하지 않음 등)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "404", description = "옵션/상품을 찾을 수 없음", content = @Content)
    })
    @PostMapping
    ResponseEntity<Void> addWishlist(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody WishlistRequestDTO requestDTO
    );

    // 2) 내 위시리스트 목록 조회
    @Operation(
            summary = "내 위시리스트 목록 조회",
            description = "로그인한 사용자의 위시리스트 목록을 최신순으로 페이징 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위시리스트 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
    })
    @GetMapping
    ResponseEntity<List<WishlistResponseDTO>> getMyWishlist(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(hidden = true)
            @PageableDefault(size = 3, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    );

    // 3) 위시리스트 삭제
    @Operation(
            summary = "위시리스트 삭제",
            description = "로그인한 사용자가 위시리스트(wishlistId)를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "위시리스트 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "본인 위시리스트가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "위시리스트를 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/{wishlistId}")
    ResponseEntity<Void> removeWishlist(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @Parameter(description = "위시리스트 ID", required = true)
            @PathVariable UUID wishlistId
    );
}