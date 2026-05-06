package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record ListingSoldOutEvent(
        Long listingId,
        Long sellerId,
        Long buyerId,
        Long price
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "LISTING";
    }

    @Override
    public Long aggregateId() {
        return listingId;
    }
}
