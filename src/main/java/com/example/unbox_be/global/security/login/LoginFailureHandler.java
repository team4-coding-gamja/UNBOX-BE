package com.example.unbox_be.global.security.login;

import com.example.unbox_be.global.error.ErrorResponse;
import com.example.unbox_be.global.error.exception.CustomAuthenticationException;
import com.example.unbox_be.global.error.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        log.error("[LoginFailureHandler] 로그인 실패: {}", failed.getMessage());

        ErrorCode errorCode = (failed instanceof CustomAuthenticationException ex)
                ? ex.getErrorCode()
                : ErrorCode.INVALID_CREDENTIALS;

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getStatus().value(), errorCode.getMessage());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}