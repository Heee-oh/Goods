package com.goods.market.chat.application.dto;

import com.goods.market.chat.domain.MessageType;

public record ChatMessageCreateDto(
        Long chatRoomId,
        MessageType type,
        String content
) {
}
