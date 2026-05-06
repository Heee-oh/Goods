package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record ListingReservationCanceledEvent(
        Long listingId,
        Long reserverId
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
