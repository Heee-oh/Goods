package com.goods.market.chat.exception;

public class ChatRoomInactiveException extends RuntimeException {
    public ChatRoomInactiveException(String message) {
        super(message);
    }
}
