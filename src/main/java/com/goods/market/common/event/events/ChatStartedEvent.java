package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record ChatStartedEvent(
        Long chatRoomId,
        Long listingId,
        Long buyerId,
        Long sellerId
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "CHAT_ROOM";
    }

    @Override
    public Long aggregateId() {
        return chatRoomId;
    }
}
