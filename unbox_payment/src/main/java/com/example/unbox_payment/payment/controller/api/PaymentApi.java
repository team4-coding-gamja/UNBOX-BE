package com.example.unbox_payment.payment.controller.api;

import com.example.unbox_payment.payment.dto.request.PaymentConfirmRequestDto;
import com.example.unbox_payment.payment.dto.request.PaymentCreateRequestDto;
import com.example.unbox_payment.payment.dto.response.PaymentHistoryResponseDto;
import com.example.unbox_payment.payment.dto.response.PaymentReadyResponseDto;
import com.example.unbox_payment.payment.dto.response.TossConfirmResponse;
import com.example.unbox_common.response.CustomApiResponse;
import com.example.unbox_common.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "[사용자] 결제 관리", description = "결제 관리 API")
@RequestMapping("/api/payment")
public interface PaymentApi {

        // ✅ 결제 이력 조회
        @Operation(summary = "결제 이력 조회", description = "로그인한 사용자의 모든 결제 내역을 조회합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "401", description = "인증 실패")
        })
        @GetMapping("/history")
        CustomApiResponse<List<PaymentHistoryResponseDto>> getPaymentHistory(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

        // ✅ 결제 준비 (초기 레코드 생성)
        @Operation(summary = "결제 생성", description = """
                        결제 초기 레코드를 생성합니다.
                        - 주문서 페이지에서 결제하기 버튼을 누를 때 호출됩니다.
                        - 결제 레디 정보(PaymentReadyResponseDto)를 반환합니다.
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "결제 생성 성공", content = @Content(schema = @Schema(implementation = PaymentReadyResponseDto.class))),
                        @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
        })
        @PostMapping("/ready")
        CustomApiResponse<PaymentReadyResponseDto> createPayment(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,

                        @RequestBody @Valid PaymentCreateRequestDto request);

        // ✅ 결제 승인 처리
        @Operation(summary = "결제 승인(확정)", description = """
                        결제를 승인(확정) 처리합니다. (실제 PG 연동)
                        - 결제 성공 후 호출되며, p_payment / p_pg_transaction 등을 업데이트합니다.
                        """)
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
                        @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 / 결제 승인 실패", content = @Content),
                        @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                        @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
                        @ApiResponse(responseCode = "404", description = "결제 정보를 찾을 수 없음", content = @Content)
        })
        @PostMapping("/confirm")
        CustomApiResponse<TossConfirmResponse> confirmPayment(
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,

                        @RequestBody @Valid PaymentConfirmRequestDto request);
}
