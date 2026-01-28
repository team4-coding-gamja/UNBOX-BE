package com.example.unbox_common.error;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;
    private final Object data;

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
