package com.example.unbox_common.error.exception;

import lombok.Getter;

@Getter
public class LockAcquisitionFailedException extends RuntimeException {
    private final String key;

    public LockAcquisitionFailedException(String key, String message) {
        super(message);
        this.key = key;
    }
}
