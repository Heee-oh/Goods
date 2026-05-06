package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record ListingCreatedEvent(
        Long listingId,
        Long sellerId,
        Integer regionId,
        String title
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
