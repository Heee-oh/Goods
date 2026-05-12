package com.goods.market.chat.application.dto;

import com.goods.market.chat.presentation.dto.ChatMessageResponse;

public record ChatMessageSendResult(
        ChatMessageResponse message,
        Long receiverId,
        boolean firstMessage
) {
}
