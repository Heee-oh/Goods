package com.goods.market.common.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.common.api.FieldErrorItem;
import com.goods.market.member.domain.exception.memberRegion.RegionVerificationExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        List<FieldErrorItem> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorItem(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        String message = fieldErrors.stream()
                .findFirst()
                .map(FieldErrorItem::message)
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("VALIDATION_FAILED", message, fieldErrors, request.getRequestURI()));
    }

    @ExceptionHandler(RegionVerificationExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegionVerificationExpired(
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(
                        "REGION_VERIFICATION_EXPIRED",
                        "지역 인증이 만료되었습니다.",
                        request.getRequestURI()
                ));
    }
    @ExceptionHandler(InternalError.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        "INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했습니다.",
                        null,
                        request.getRequestURI()
                ));
    }
}
