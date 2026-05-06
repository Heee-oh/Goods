package com.goods.market.member.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.listing.exception.ListingBadRequestException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionAlreadyExistsException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionLimitExceededException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionNotFoundException;
import com.goods.market.member.domain.exception.memberRegion.MemberRegionVerificationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.member")
public class MemberExceptionHandler {

    @ExceptionHandler(MemberRegionAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegionAlreadyExists(MemberRegionAlreadyExistsException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MEMBER_REGION_ALREADY_EXISTS", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MemberRegionLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegionLimitExceeded(MemberRegionLimitExceededException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MEMBER_REGION_LIMIT_EXCEEDED", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MemberRegionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegionNotFound(MemberRegionNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "MEMBER_REGION_NOT_FOUND", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MemberRegionVerificationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegionVerificationFailed(MemberRegionVerificationFailedException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "MEMBER_REGION_VERIFICATION_FAILED", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ListingBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleProfileImageBadRequest(ListingBadRequestException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "PROFILE_IMAGE_BAD_REQUEST", e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, null, path));
    }
}
