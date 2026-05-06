package com.goods.market.chat.presentation.dto;

import com.goods.market.chat.domain.MessageType;

public record ChatMessageRequest(Long chatRoomId,  MessageType type, String content) {
}
