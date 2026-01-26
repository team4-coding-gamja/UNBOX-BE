package com.example.unbox_user.user.dto.response;

import com.example.unbox_user.user.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDto {

    @Schema(description = "계좌 ID")
    private UUID id;

    @Schema(description = "은행명")
    private String bankName;

    @Schema(description = "계좌번호")
    private String accountNumber;

    @Schema(description = "예금주")
    private String accountHolder;

    @Schema(description = "기본 계좌 여부")
    private boolean isDefault;

    public static AccountResponseDto from(Account account) {
        return AccountResponseDto.builder()
                .id(account.getId())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .isDefault(account.isDefault())
                .build();
    }
}
