package com.example.unbox_be.domain.user.controller.api;

import com.example.unbox_be.domain.user.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserSignupResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "회원 관리", description = "회원 관련 API")
public interface UserApi {

    @Operation(
            summary = "회원가입",
            description =
                    "이메일/비밀번호/닉네임/전화번호로 회원가입을 진행합니다.\n" +
                            "```json\n" +
                            "{\n" +
                            "  \"email\": \"test@example.com\",\n" +
                            "  \"password\": \"Password!\",\n" +
                            "  \"nickname\": \"tester\",\n" +
                            "  \"phone\": \"01012345678\"\n" +
                            "}\n" +
                            "```"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserSignupResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "회원가입 실패(중복 등)",
                    content = @Content(schema = @Schema(type = "string", example = "이미 존재하는 사용자입니다."))
            )
    })
    @PostMapping("/api/auth/signup")
    ResponseEntity<UserSignupResponseDto> register(
            @Parameter(description = "회원가입 요청 DTO", required = true)
            @RequestBody UserSignupRequestDto userSignupRequestDto
    );
}
