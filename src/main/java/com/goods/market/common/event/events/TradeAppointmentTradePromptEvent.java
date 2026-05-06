package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;

public record TradeAppointmentTradePromptEvent(
        Long appointmentId,
        Long listingId,
        Long sellerId,
        Long buyerId
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "TRADE";
    }

    @Override
    public Long aggregateId() {
        return appointmentId;
    }
}
