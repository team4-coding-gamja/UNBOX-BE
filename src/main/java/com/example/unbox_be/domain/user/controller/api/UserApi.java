package com.example.unbox_be.domain.user.controller.api;

import com.example.unbox_be.domain.user.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.dto.response.UserSignupResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "회원 API")
@RequestMapping("/api/auth")
public interface UserApi {

    @Operation(
            summary = "회원가입",
            description = "이메일/비밀번호/닉네임/전화번호로 회원가입을 진행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponseDto.class),
                            examples = @ExampleObject(
                                    name = "성공 예시",
                                    value = """
                                            {
                                              "id": 1,
                                              "email": "test@example.com",
                                              "nickname": "tester",
                                              "phone": "01012345678"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이메일 중복 (USER_ALREADY_EXISTS)",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "중복 예시",
                                    value = """
                                            {
                                              "code": "USER_ALREADY_EXISTS",
                                              "message": "이미 존재하는 사용자입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/signup")
    ResponseEntity<UserSignupResponseDto> signup(
            @RequestBody(
                    required = true,
                    description = "회원가입 요청 데이터",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupRequestDto.class),
                            examples = @ExampleObject(
                                    name = "요청 예시",
                                    value = """
                                            {
                                              "email": "test@example.com",
                                              "password": "P@ssw0rd!",
                                              "nickname": "tester",
                                              "phone": "01012345678"
                                            }
                                            """
                            )
                    )
            )
            UserSignupRequestDto requestDto
    );
}
