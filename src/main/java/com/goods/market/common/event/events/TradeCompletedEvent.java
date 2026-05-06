package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record TradeCompletedEvent(
        Long tradeId,
        Long listingId,
        Long sellerId,
        Long buyerId,
        Long price
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "TRADE";
    }

    @Override
    public Long aggregateId() {
        return tradeId;
    }
}
