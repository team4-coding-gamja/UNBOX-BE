package com.example.unbox_be.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통 (Common)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    DATA_INTEGRITY_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 무결성 오류가 발생했습니다."),

    // 로그인 / 회원가입 (User/Auth)
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용중인 아이디(이메일)입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    USERNAME_OR_PASSWORD_MISSING(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 입력하세요."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),

    // 인증 및 권한 (Security/JWT)
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "로그인 요청을 처리하는 동안 오류가 발생했습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증 토큰이 누락되었습니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 누락되었거나 Bearer 타입이 아닙니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해주세요."),
    TOKEN_LOGOUT(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),

    // 주문 관련 에러
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 정보를 찾을 수 없습니다."),
    PRODUCT_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "상품 옵션을 찾을 수 없습니다."),
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "입찰 정보를 찾을 수 없습니다."),
    INVALID_BID_PRICE(HttpStatus.BAD_REQUEST, "입찰 가격이 유효하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문 내역을 찾을 수 없습니다."),
    PRICE_MISMATCH(HttpStatus.BAD_REQUEST, "주문 가격이 실제 판매 가격과 일치하지 않습니다."),
    SELLING_BID_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 판매자가 판매 중인 상품이 아닙니다."),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.BAD_REQUEST, "이미 배송 중이거나 완료된 주문은 취소할 수 없습니다."),

    // 리뷰 관련 에러 (Review)
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰를 찾을 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 해당 주문에 대한 리뷰를 작성했습니다."),
    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰만 수정/삭제할 수 있습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "평점은 1점에서 5점 사이여야 합니다.");

    private final HttpStatus status;
    private final String message;
}

