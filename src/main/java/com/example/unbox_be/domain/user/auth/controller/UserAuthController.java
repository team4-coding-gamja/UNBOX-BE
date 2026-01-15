package com.example.unbox_be.domain.user.auth.controller;

import com.example.unbox_be.domain.user.auth.controller.api.UserAuthApi;
import com.example.unbox_be.domain.user.auth.dto.request.UserLoginRequestDto;
import com.example.unbox_be.domain.user.auth.dto.response.UserTokenResponseDto;
import com.example.unbox_be.domain.user.auth.dto.request.UserSignupRequestDto;
import com.example.unbox_be.domain.user.auth.dto.response.UserSignupResponseDto;
import com.example.unbox_be.domain.user.auth.service.UserAuthService;
import com.example.unbox_be.global.response.CustomApiResponse;
import com.example.unbox_be.global.security.jwt.JwtConstants;
import com.example.unbox_be.global.security.jwt.JwtUtil;
import com.example.unbox_be.global.security.token.RefreshTokenRedisRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController implements UserAuthApi {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final UserAuthService userAuthService;

    // ✅ 회원가입
    @PostMapping("/signup")
    public CustomApiResponse<UserSignupResponseDto> signup(
            @Valid @RequestBody UserSignupRequestDto requestDto) {
        UserSignupResponseDto userResponseDto = userAuthService.signup(requestDto);
        return CustomApiResponse.success(userResponseDto);
    }

    // ✅ 로그인 (실제 로그인 로직은 LoginFilter에서 처리)
    @PostMapping("/login")
    public ResponseEntity<String> login(UserLoginRequestDto requestDto) {
        log.info("[AuthController] 로그인 요청: {}", requestDto.getEmail());
        return ResponseEntity.ok("로그인 요청이 처리되었습니다.");
    }

    // ✅ 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        log.info("[AuthController/logout] 로그아웃 요청 처리");
        // 실제 로그아웃 처리는 JWTFilter에서 수행
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    // ✅ 토큰 재발급 (엑세스 토큰 만료 시 리프레시 토큰을 통해 새로 생성하기)
    @PostMapping("/reissue")
    public ResponseEntity<UserTokenResponseDto> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 리프레시 토큰 가져오기 (쿠키 또는 헤더에서)
        String refreshToken = extractRefreshToken(request);

        // 리프레시 토큰이 없으면 오류 응답
        if (refreshToken == null) {
            log.info("[AuthController/reissue] 리프레시 토큰을 찾을 수 없음");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // 만료 여부 확인
        if (jwtUtil.isExpired(refreshToken)) {
            log.info("[AuthController/reissue] 리프레시 토큰이 만료됨");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        // 사용자 정보 가져오기
        String username = jwtUtil.getUsername(refreshToken);

        // 사용자 아이디
        Long userId = jwtUtil.getUserId(refreshToken);

        // 역할 정보 설정 (기본값 사용)
        String role = "ROLE_USER";

        // 새로운 액세스 토큰 생성
        String newAccessToken = jwtUtil.createAccessToken(userId, username, role, JwtConstants.ACCESS_TOKEN_EXPIRE_MS);
        String newRefreshToken = jwtUtil.createRefreshToken(userId, username, role, JwtConstants.REFRESH_TOKEN_EXPIRE_MS);

        // RefreshToken 저장 (Redis)
        refreshTokenRedisRepository.saveRefreshToken(username, newRefreshToken);

        // 새로운 TokenDTO 객체 생성
        UserTokenResponseDto userTokenResponseDto = UserTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();

        // Authorization 헤더에 추가
        response.addHeader("Authorization", "Bearer " + newAccessToken);

        // 쿠키에 리프레시 토큰 추가
        Cookie refreshCookie = createCookie("refresh", newRefreshToken);
        response.addCookie(refreshCookie);

        log.info("[AuthController/reissue] 토큰 재발급 성공 - 사용자: {}", username);
        return new ResponseEntity<>(userTokenResponseDto, HttpStatus.OK);
    }

    /**
     * 요청에서 리프레시 토큰을 추출하는 메서드
     */
    private String extractRefreshToken(HttpServletRequest request) {
        // 1. 쿠키에서 리프레시 토큰 찾기
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Authorization 헤더에서도 확인
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 이후의 문자열

            // 토큰이 유효하고 사용자 정보가 포함되어 있으면 사용
            if (!jwtUtil.isExpired(token) && jwtUtil.getUsername(token) != null) {
                return token;
            }
        }

        // 3. 요청 파라미터에서 확인
        String tokenParam = request.getParameter("refreshToken");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // 리프레시 토큰을 찾지 못함
        return null;
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 1일 동안 유지
        cookie.setPath("/"); // 모든 경로에서 접근 가능하도록 설정
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가능하도록 설정 (보안 강화)

        // HTTPS 환경이 아니라면 secure 설정 주석 처리
        // cookie.setSecure(true);

        return cookie;
    }
}
