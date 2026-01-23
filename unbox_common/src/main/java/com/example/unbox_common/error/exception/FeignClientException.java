package com.example.unbox_common.error.exception;

import lombok.Getter;

@Getter
public class FeignClientException extends RuntimeException {
    private final int status;
    private final String message;
    private final Object data;

    public FeignClientException(int status, String message) {
        this(status, message, null);
    }

    public FeignClientException(int status, String message, Object data) {
        super(message);
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
