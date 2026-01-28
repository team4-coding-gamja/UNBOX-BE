package com.example.unbox_user.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequestDto {

    @Schema(description = "은행명", example = "토스뱅크")
    @NotBlank(message = "은행명은 필수입니다.")
    private String bankName;

    @Schema(description = "계좌번호", example = "1000-00-123456")
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @Schema(description = "예금주", example = "홍길동")
    @NotBlank(message = "예금주는 필수입니다.")
    private String accountHolder;

    @Schema(description = "기본 계좌 여부", example = "true")
    private boolean isDefault;
}
