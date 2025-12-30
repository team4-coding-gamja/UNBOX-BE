package com.example.unbox_be.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // --- 공통 (Common) ---
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // --- 로그인 / 회원가입 (User/Auth) ---
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 아이디(이메일)입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    USERNAME_OR_PASSWORD_MISSING(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 입력하세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),

    // --- 인증 및 권한 (Security/JWT) ---
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "로그인 요청을 처리하는 동안 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증 토큰이 누락되었습니다."),

    // --- 비즈니스 로직 (KREAM 스타일 C2C 관련) ---
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 정보를 찾을 수 없습니다."),
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "입찰 정보를 찾을 수 없습니다."),
    INVALID_BID_PRICE(HttpStatus.BAD_REQUEST, "입찰 가격이 유효하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 내역을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}

