package com.goods.market.chat.presentation.dto;

import com.goods.market.chat.domain.MessageType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;

public record ChatMessageResponse(
        Long chatRoomId,
        Long MessageId,
        @JsonSerialize(using = ToStringSerializer.class)
        Long senderId,
        MessageType type,
        String content,
        Instant createAt
) {
}
