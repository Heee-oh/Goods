package com.goods.market.review.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.review.exception.ReviewAccessDeniedException;
import com.goods.market.review.exception.ReviewAlreadyExistsException;
import com.goods.market.review.exception.ReviewExchangeNotCompletedException;
import com.goods.market.review.exception.ReviewExchangeNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.review")
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewExchangeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleExchangeNotFound(ReviewExchangeNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "REVIEW_EXCHANGE_NOT_FOUND", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReviewExchangeNotCompletedException.class)
    public ResponseEntity<ApiResponse<Void>> handleExchangeNotCompleted(ReviewExchangeNotCompletedException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "REVIEW_EXCHANGE_NOT_COMPLETED", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(ReviewAccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyExists(ReviewAlreadyExistsException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, message, null, path));
    }
}
