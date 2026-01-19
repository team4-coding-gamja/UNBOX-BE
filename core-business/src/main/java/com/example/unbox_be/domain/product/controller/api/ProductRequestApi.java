package com.example.unbox_be.domain.product.controller.api;

import com.example.unbox_be.domain.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_be.domain.product.dto.request.ProductRequestRequestDto;
import com.example.unbox_be.domain.product.dto.response.ProductRequestResponseDto;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.auth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상품 등록 요청 관리", description = "상품 등록 요청 관리 API")
@RequestMapping("/api/products")
public interface ProductRequestApi {

    @Operation(
            summary = "상품 등록 요청 생성",
            description = """
                    사용자가 상품 등록 요청을 생성합니다.
                    - 로그인 필요(JWT)
                    - 요청 DTO는 name, brandName만 받습니다. (ID는 서버에서 생성)
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ProductRequestRequestDto.class),
                    examples = {
                            @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "퓨마 스피드캣 등록 요청",
                                    value = """
                                                    {
                                                      "name": "puma",
                                                      "brandName": "speedcat"
                                                    }
                                                    """
                            )

                    }
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "상품 등록 요청 생성 성공",
                    content = @Content(schema = @Schema(implementation = CustomApiResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/requests")
    CustomApiResponse<ProductRequestResponseDto> createProductRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid ProductRequestRequestDto requestDto
    );
}
