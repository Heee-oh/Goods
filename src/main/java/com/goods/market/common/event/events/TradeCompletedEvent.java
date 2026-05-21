package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;
import com.goods.market.trade.domain.Price;

public record TradeCompletedEvent(
        Long tradeId,
        Long listingId,
        Long sellerId,
        Long buyerId,
        Price price
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
