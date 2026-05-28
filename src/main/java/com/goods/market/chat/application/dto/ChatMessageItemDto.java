package com.goods.market.chat.application.dto;

import com.goods.market.chat.domain.MessageType;

import java.time.Instant;

public record ChatMessageItemDto(
        Long messageId,
        Long senderId,
        MessageType type,
        String content,
        Instant createdAt
) {
}
