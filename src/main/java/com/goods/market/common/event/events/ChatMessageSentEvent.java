package com.goods.market.common.event.events;

import com.goods.market.chat.domain.MessageType;
import com.goods.market.common.event.DomainEvent;

public record ChatMessageSentEvent(
        Long messageId,
        Long chatRoomId,
        Long senderId,
        Long otherMemberId,
        MessageType type,
        String content
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "CHAT_MESSAGE";
    }

    @Override
    public Long aggregateId() {
        return messageId;
    }
}
