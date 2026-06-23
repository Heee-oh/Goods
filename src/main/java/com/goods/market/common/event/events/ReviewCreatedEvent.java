package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record ReviewCreatedEvent(
        Long reviewId,
        Long sellerId,
        Long purchaserId,
        int rating
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "REVIEW";
    }

    @Override
    public Long aggregateId() {
        return reviewId;
    }
}
