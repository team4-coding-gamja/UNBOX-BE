package com.example.unbox_common.error;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  // Jackson 역직렬화용 기본 생성자
public class ErrorResponse {

    private int status;
    private String message;
    private Object data;

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }

    public ErrorResponse(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message);
    }

    public static ErrorResponse of(int status, String message, Object data) {
        return new ErrorResponse(status, message, data);
    }
}
