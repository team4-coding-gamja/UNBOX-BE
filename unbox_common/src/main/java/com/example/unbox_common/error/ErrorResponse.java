package com.example.unbox_common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    private final int status;
    private final String message;

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message);
    }
}
