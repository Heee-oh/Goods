package com.goods.market.chat.presentation;

import com.goods.market.common.api.ApiResponse;
import com.goods.market.chat.exception.ChatReadNotFoundException;
import com.goods.market.chat.exception.ChatRoomInactiveException;
import com.goods.market.chat.exception.ChatRoomNotFoundException;
import com.goods.market.chat.exception.ChatRoomParticipantException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.goods.market.chat")
public class ChatExceptionHandler {

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ChatRoomNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ChatRoomParticipantException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ChatRoomParticipantException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "CHAT_ROOM_FORBIDDEN", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ChatRoomInactiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ChatRoomInactiveException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CHAT_ROOM_INACTIVE", e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ChatReadNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleReadNotFound(ChatReadNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "CHAT_READ_NOT_FOUND", e.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiResponse.failure(code, message, null, path));
    }
}
