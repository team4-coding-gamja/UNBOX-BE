package com.example.unbox_be.domain.payment.controller.api;

import com.example.unbox_be.domain.payment.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_be.domain.payment.dto.request.PaymentCreateRequestDto;
import com.example.unbox_be.domain.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_be.domain.payment.dto.response.TossConfirmResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "결제 관리", description = "결제 관리 API")
@RequestMapping("/api/payment")
public interface PaymentApi {

    // ✅ 1) 결제 초기 레코드 생성
    @Operation(
            summary = "결제 생성",
            description = """
                    결제 초기 레코드를 생성합니다.
                    - 주문서 페이지에서 결제하기 버튼을 누를 때 호출됩니다.
                    - 결제 레디 정보(PaymentReadyResponseDto)를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "결제 생성 성공",
                    content = @Content(schema = @Schema(implementation = PaymentReadyResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    @PostMapping("/ready")
    ResponseEntity<PaymentReadyResponseDto> createPayment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid PaymentCreateRequestDto request
    );


    // ✅ 2) 결제 승인 처리 (Mock)
    @Operation(
            summary = "결제 승인(확정)",
            description = """
                    결제를 승인(확정) 처리합니다. (Mock)
                    - 결제 성공 후 호출되며, p_payment / p_pg_transaction 등을 업데이트합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 결제 승인 실패", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음", content = @Content)
    })
    @PostMapping("/confirm")
    ResponseEntity<TossConfirmResponse> confirmPayment(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid PaymentConfirmRequestDto request
    );
}