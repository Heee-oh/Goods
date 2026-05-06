package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;
import java.time.Instant;

public record TradeAppointmentScheduledEvent(
        Long appointmentId,
        Long listingId,
        Long sellerId,
        Long buyerId,
        Instant notificationTime
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
