package com.goods.market.trade.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.trade.exception.AppointmentBadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.trade")
public class AppointmentExceptionHandler {

    @ExceptionHandler(AppointmentBadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            AppointmentBadRequestException e,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("APPOINTMENT_BAD_REQUEST", e.getMessage(), null, request.getRequestURI()));
    }
}
