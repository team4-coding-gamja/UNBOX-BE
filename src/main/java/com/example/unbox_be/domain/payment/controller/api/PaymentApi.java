package com.example.unbox_be.domain.payment.controller.api;

import com.example.unbox_be.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_be.domain.payment.dto.request.PaymentCreateRequestDto;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Payment API", description = "결제 및 PG 트랜잭션 관리 API")
public interface PaymentApi {
    @Operation(summary = "결제 초기 레코드 생성", description = "주문서 페이지에서 결제하기 버튼을 누를 때 호출됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "결제 레코드 생성 성공"),
            @ApiResponse(responseCode = "404", description = "주문 정보를 찾을 수 없음")
    })
    ResponseEntity<PaymentReadyResponseDto> createPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PaymentCreateRequestDto request
    );

    @Operation(summary = "결제 승인 처리", description = "PG 승인 성공 후 호출하면 결제 및 트랜잭션이 업데이트됩니다.")
    ResponseEntity<Void> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid PaymentConfirmRequestDto request
    );
}
