package com.example.unbox_user.user.controller;

import com.example.unbox_common.security.auth.CustomUserDetails;
import com.example.unbox_user.user.dto.request.AccountCreateRequestDto;
import com.example.unbox_user.user.dto.response.AccountResponseDto;
import com.example.unbox_user.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "[사용자] 계좌 관리", description = "사용자 정산 계좌 관리 API")
@RestController
@RequestMapping("/users/me/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 등록", description = "새로운 정산 계좌를 등록합니다.")
    @PostMapping
    public ResponseEntity<AccountResponseDto> registerAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AccountCreateRequestDto request) {
        return ResponseEntity.ok(accountService.registerAccount(userDetails.getUserId(), request));
    }

    @Operation(summary = "내 계좌 목록 조회", description = "등록된 모든 계좌 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<AccountResponseDto>> getMyAccounts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(accountService.getMyAccounts(userDetails.getUserId()));
    }

    @Operation(summary = "계좌 삭제", description = "등록된 계좌를 삭제합니다. (Soft Delete)")
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID accountId) {
        accountService.deleteAccount(userDetails.getUserId(), accountId);
        return ResponseEntity.noContent().build();
    }
}
