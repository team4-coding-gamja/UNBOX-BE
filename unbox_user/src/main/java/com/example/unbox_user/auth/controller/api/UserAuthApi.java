package com.example.unbox_user.auth.controller.api;

import com.example.unbox_user.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_user.auth.dto.response.UserTokenResponseDto;
import com.example.unbox_user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_user.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_common.response.CustomApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "사용자 인증", description = "사용자 회원가입 / 로그인 / 로그아웃 / 토큰 재발급 API")
public interface UserAuthApi {

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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = UserSignupRequestDto.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "구매자 1 회원가입",
                                    value = """
                                                    {
                                                      "email": "buyer1@unbox.com",
                                                      "password": "12341234!",
                                                      "nickname": "buyer1",
                                                      "phone": "010-1234-5678"
                                                    }
                                                    """
                            ),
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "판매자 1 회원가입",
                                    value = """
                                                    {
                                                      "email": "seller1@unbox.com",
                                                      "password": "12341234!",
                                                      "nickname": "seller1",
                                                      "phone": "010-2345-6781"
                                                    }
                                                    """
                            )

                    }
            )
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
    CustomApiResponse<UserSignupResponseDto> signup(
            @Parameter(description = "회원가입 요청 DTO", required = true)
            @RequestBody UserSignupRequestDto userSignupRequestDto
    );

    @Operation(
            summary = "일반 로그인 (ID/PW)",
            description =
                    """
                    ID/PW 로그인 요청입니다. 실제 인증 및 토큰 발급은 Spring Security LoginFilter + SuccessHandler에서 처리됩니다.
                    
                    ✅ 성공 시
                    - 응답 헤더: Authorization: Bearer {accessToken}
                    - 응답 쿠키: refresh={refreshToken} (HttpOnly)
                    - 응답 바디: accessToken/refreshToken JSON
                    
                    ✅ Swagger 테스트 방법
                    1) 이 API를 실행
                    2) 응답 헤더의 Authorization 값을 복사
                    3) Swagger 우측 상단 Authorize 버튼에 붙여넣고 보호 API 호출
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer access token", schema = @Schema(type = "string"))
                    },
                    content = @Content(schema = @Schema(implementation = UserTokenResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "로그인 실패(아이디/비밀번호 불일치)",
                    content = @Content(schema = @Schema(type = "string", example = "아이디 또는 비밀번호가 일치하지 않습니다."))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 누락/형식 오류",
                    content = @Content(schema = @Schema(type = "string", example = "잘못된 요청입니다."))
            )
    })
    @PostMapping("/login")
    ResponseEntity<String> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UserLoginRequestDto.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "구매자 1 로그인",
                                            value = """
                                                    {
                                                      "email": "buyer1@unbox.com",
                                                      "password": "12341234!"
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "판매자 1 로그인",
                                            value = """
                                                    {
                                                      "email": "seller1@unbox.com",
                                                      "password": "12341234!"
                                                    }
                                                    """
                                    )

                            }
                    )
            )
            UserLoginRequestDto userLoginRequestDto
    );

    @Operation(
            summary = "로그아웃",
            description =
                    """
                    로그아웃 요청입니다.
                    일반적으로 다음 처리를 수행합니다.
                    - access token 블랙리스트 처리(선택)
                    - Redis에서 refresh token 삭제
                    - refresh 쿠키 만료
                    
                    ⚠️ 실제 로그아웃 처리는 프로젝트 구조에 따라 CustomLogoutFilter에서 처리될 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(schema = @Schema(type = "string", example = "로그아웃 되었습니다."))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "토큰이 없거나 유효하지 않음",
                    content = @Content(schema = @Schema(type = "string", example = "토큰이 없습니다. 로그아웃을 진행할 수 없습니다."))
            )
    })
    @PostMapping("/logout")
    ResponseEntity<String> logout(
            @Parameter(hidden = true) HttpServletRequest request
    );

    @Operation(
            summary = "토큰 재발급",
            description =
                    """
                    AccessToken 만료 시 RefreshToken으로 새로운 토큰을 발급합니다.
                    
                    ✅ 요청
                    - refresh 쿠키(HttpOnly)가 자동 전송되는 방식 권장
                    
                    ✅ 응답
                    - Authorization 헤더에 새 access token
                    - refresh 쿠키 갱신
                    - 바디에 accessToken/refreshToken JSON
                    
                    ✅ Swagger 테스트 팁
                    - Swagger UI는 쿠키 자동 핸들링이 환경에 따라 안 될 수 있습니다.
                    - 같은 브라우저 세션에서 로그인 후, reissue 호출을 하면 refresh 쿠키가 같이 전송되는 경우가 많습니다.
                    - 안 되면, 브라우저 개발자도구(Application/Storage → Cookies)에서 refresh 쿠키 존재 여부를 확인하세요.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공",
                    headers = {
                            @Header(name = "Authorization", description = "Bearer access token", schema = @Schema(type = "string"))
                    },
                    content = @Content(schema = @Schema(implementation = UserTokenResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "refresh token 없음",
                    content = @Content(schema = @Schema(type = "string", example = "리프레시 토큰을 찾을 수 없음"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "refresh token 만료/유효하지 않음",
                    content = @Content(schema = @Schema(type = "string", example = "리프레시 토큰이 만료됨"))
            )
    })
    @PostMapping("/reissue")
    ResponseEntity<UserTokenResponseDto> reissue(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response
    );
}

