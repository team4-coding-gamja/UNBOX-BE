package com.example.unbox_be.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 로그인 / 회원가입
    SAMPLE_ERROR(HttpStatus.BAD_REQUEST, "Sample Error Message"),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 아이디(이메일)입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    USERNAME_OR_PASSWORD_MISSING(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 입력하세요."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "로그인 요청을 처리하는 동안 오류가 발생했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
