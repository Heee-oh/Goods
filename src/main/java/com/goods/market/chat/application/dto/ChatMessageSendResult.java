package com.goods.market.chat.application.dto;

public record ChatMessageSendResult(
        ChatMessageDto message,
        Long receiverId,
        boolean firstMessage
) {
}
