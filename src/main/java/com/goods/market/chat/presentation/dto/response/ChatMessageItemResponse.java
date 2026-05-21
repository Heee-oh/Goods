package com.goods.market.chat.presentation.dto.response;

import com.goods.market.chat.application.dto.ChatMessageItemDto;
import com.goods.market.chat.domain.MessageType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

public record ChatMessageItemResponse(
        Long messageId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long senderId,
        MessageType type,
        String content,
        Instant createdAt
) {
    public static ChatMessageItemResponse from(ChatMessageItemDto dto) {
        return new ChatMessageItemResponse(
                dto.messageId(),
                dto.senderId(),
                dto.type(),
                dto.content(),
                dto.createdAt()
        );
    }
}
