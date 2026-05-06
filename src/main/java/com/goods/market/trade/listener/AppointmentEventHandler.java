package com.goods.market.trade.listener;

import com.goods.market.common.event.events.TradeCompletedEvent;
import com.goods.market.trade.domain.AppointmentStatus;
import com.goods.market.trade.infrastructure.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentEventHandler {

    private final AppointmentRepository appointmentRepository;

    @EventListener
    public void handle(TradeCompletedEvent event) {
        appointmentRepository.findTopByListingIdAndBuyerIdAndStatusOrderByCreatedAtDesc(
                        event.listingId(),
                        event.buyerId(),
                        AppointmentStatus.SCHEDULED
                )
                .ifPresent(appointment -> {
                    appointment.markDone();
                    appointment.dismissTradePrompt();
                });
    }
}
