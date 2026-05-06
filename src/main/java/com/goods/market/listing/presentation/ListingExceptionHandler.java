package com.goods.market.listing.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.listing.exception.ListingAccessDeniedException;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.listing.exception.ListingConflictException;
import com.goods.market.listing.exception.ListingNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.listing")
public class ListingExceptionHandler {

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ListingNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ListingAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(ListingAccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "LISTING_ACCESS_DENIED", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ListingBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(ListingBadRequestException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "LISTING_BAD_REQUEST", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ListingConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ListingConflictException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "LISTING_CONFLICT", e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, null, path));
    }
}
