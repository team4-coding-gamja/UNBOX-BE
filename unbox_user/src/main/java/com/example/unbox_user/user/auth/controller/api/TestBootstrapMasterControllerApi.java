package com.example.unbox_user.user.auth.controller.api;

import com.example.unbox_user.user.auth.controller.TestBootstrapMasterController.CreateMasterRequest;
import com.example.unbox_user.user.auth.dto.response.AdminSignupResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(
        name = "⚠️ 테스트 / 부트스트랩",
        description = "초기 ROLE_MASTER 계정 생성을 위한 테스트 전용 API (운영 시 반드시 삭제)"
)
public interface TestBootstrapMasterControllerApi {

    @Operation(
            summary = "테스트용 마스터 계정 생성",
            description = """
                    ⚠️ 테스트 환경 전용 API
                    
                    - ROLE_MASTER 계정을 1회만 생성 가능
                    - 이미 존재하면 409 반환
                    - role/status는 서버에서 고정 처리
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "마스터 계정 생성 성공",
                    content = @Content(
                            schema = @Schema(implementation = AdminSignupResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 ROLE_MASTER 존재",
                    content = @Content(
                            schema = @Schema(type = "string", example = "이미 ROLE_MASTER 계정이 존재합니다.")
                    )
            )
    })
    @PostMapping("/api/test/bootstrap/master")
    ResponseEntity<?> createMaster(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateMasterRequest.class),
                            examples = @ExampleObject(
                                    name = "마스터 생성 요청 예시",
                                    value = """
                                            {
                                              "email": "master@test.com",
                                              "password": "Test1234!",
                                              "nickname": "master",
                                              "phone": "010-1234-5678"
                                            }
                                            """
                            )
                    )
            )
            CreateMasterRequest request
    );
}
