package com.example.unbox_user.user.service;

import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_user.user.dto.request.AccountCreateRequestDto;
import com.example.unbox_user.user.dto.response.AccountResponseDto;
import com.example.unbox_user.user.entity.Account;
import com.example.unbox_user.user.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponseDto registerAccount(Long userId, AccountCreateRequestDto request) {
        // 첫 계좌인 경우 자동으로 기본 계좌로 설정
        boolean isFirstAccount = accountRepository.findAllByUserId(userId).isEmpty();
        boolean isDefault = request.isDefault() || isFirstAccount;

        if (isDefault) {
            // 기존 기본 계좌가 있다면 해제
            accountRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(account -> account.updateDefault(false));
        }

        Account account = Account.create(
                userId,
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolder(),
                isDefault
        );

        return AccountResponseDto.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getMyAccounts(Long userId) {
        return accountRepository.findAllByUserId(userId).stream()
                .map(AccountResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAccount(Long userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        account.softDelete("USER_REQUEST");
    }
}
