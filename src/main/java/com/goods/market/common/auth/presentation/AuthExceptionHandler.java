package com.goods.market.common.auth.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.auth.exception.AuthBadRequestException;
import com.goods.market.common.auth.exception.AuthConflictException;
import com.goods.market.common.auth.exception.AuthUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.common.auth")
public class AuthExceptionHandler {

    @ExceptionHandler(AuthBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(AuthBadRequestException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "AUTH_BAD_REQUEST", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(AuthConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "AUTH_CONFLICT", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AuthUnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(AuthUnauthorizedException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, null, path));
    }
}
