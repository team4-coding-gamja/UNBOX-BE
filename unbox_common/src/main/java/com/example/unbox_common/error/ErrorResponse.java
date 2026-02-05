package com.example.unbox_common.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ErrorResponse {

    private final int status;
    private final String message;
    private final Object data;

    @JsonCreator
    public ErrorResponse(
            @JsonProperty("status") int status,
            @JsonProperty("message") String message) {
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
