package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;
import com.goods.market.listing.domain.Price;

public record ListingSoldOutEvent(
        Long listingId,
        Long sellerId,
        Long buyerId,
        Price price
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
