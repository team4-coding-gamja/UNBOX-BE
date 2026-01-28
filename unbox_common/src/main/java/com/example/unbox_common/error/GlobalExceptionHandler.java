package com.example.unbox_common.error;

import com.example.unbox_common.error.exception.CustomAuthenticationException;
import com.example.unbox_common.error.exception.CustomException;
import com.example.unbox_common.error.exception.ErrorCode;
import com.example.unbox_common.error.exception.FeignClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 일반적인 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage());
        return new ResponseEntity<>(response, errorCode.getStatus());
    }

    // 유효성 검사 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // 인증 예외 처리
    @ExceptionHandler(CustomAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(CustomAuthenticationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage()));
    }

    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, ex.getMessage()));
    }

    // 데이터 무결성 예외 처리 (Unique Key 중복 등)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "데이터 처리 중 충돌이 발생했습니다. (중복된 데이터 등)"));
    }

    // 결과가 유일하지 않은 경우 (데이터 꼬임)
    @ExceptionHandler(jakarta.persistence.NonUniqueResultException.class)
    public ResponseEntity<ErrorResponse> handleNonUniqueResult(jakarta.persistence.NonUniqueResultException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "서버 내부 데이터 오류가 발생했습니다. (중복된 결과)"));
    }

    // FeignClientException 처리
    @ExceptionHandler(FeignClientException.class)
    public ResponseEntity<ErrorResponse> handleFeignClientException(FeignClientException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), ex.getMessage(), ex.getData()));
    }
}
