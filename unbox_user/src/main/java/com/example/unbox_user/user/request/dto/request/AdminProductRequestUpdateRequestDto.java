package com.example.unbox_user.user.request.dto.request;

import com.example.unbox_user.user.request.entity.ProductRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductRequestUpdateRequestDto {

    @NotNull(message = "상태는 필수입니다.")
    private ProductRequestStatus status;
}
