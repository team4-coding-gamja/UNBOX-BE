package com.example.unbox_user.user.controller;

import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_user.user.dto.request.AddressCreateRequestDto;
import com.example.unbox_user.user.dto.response.AddressResponseDto;
import com.example.unbox_user.user.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "[사용자] 배송지 관리", description = "사용자 배송지 관리 API")
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "배송지 등록", description = "새로운 배송지를 등록합니다.")
    @PostMapping
    public ResponseEntity<AddressResponseDto> registerAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddressCreateRequestDto request) {
        return ResponseEntity.ok(addressService.registerAddress(userDetails.getUserId(), request));
    }

    @Operation(summary = "내 배송지 목록 조회", description = "등록된 모든 배송지 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<AddressResponseDto>> getMyAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(addressService.getMyAddresses(userDetails.getUserId()));
    }

    @Operation(summary = "배송지 삭제", description = "등록된 배송지를 삭제합니다. (Soft Delete)")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID addressId) {
        addressService.deleteAddress(userDetails.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
