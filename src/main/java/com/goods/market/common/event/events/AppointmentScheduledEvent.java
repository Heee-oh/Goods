package com.goods.market.common.event.events;

import com.goods.market.common.event.DomainEvent;
import java.time.Instant;

public record AppointmentScheduledEvent(
        Long appointmentId,
        Long listingId,
        Long sellerId,
        Long buyerId,
        Instant alarmTime
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "APPOINTMENT";
    }

    @Override
    public Long aggregateId() {
        return appointmentId;
    }
}
