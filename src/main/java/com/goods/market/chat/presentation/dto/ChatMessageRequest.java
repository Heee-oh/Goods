package com.goods.market.chat.presentation.dto;

import com.goods.market.chat.application.dto.ChatMessageCreateDto;
import com.goods.market.chat.domain.MessageType;

public record ChatMessageRequest(Long chatRoomId,  MessageType type, String content) {
    public ChatMessageCreateDto toDto() {
        return new ChatMessageCreateDto(chatRoomId, type, content);
    }
}
