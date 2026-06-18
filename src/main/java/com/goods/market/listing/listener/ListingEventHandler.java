package com.goods.market.listing.listener;

import com.goods.market.common.event.events.TradeAppointmentCanceledEvent;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.ListingRepository;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.exception.ListingConflictException;
import com.goods.market.listing.exception.ListingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingEventHandler {

    private final ListingRepository listingRepository;

    @EventListener
    public void handle(TradeAppointmentScheduledEvent event) {
        Listing listing = listingRepository.findActiveById(event.listingId())
                .orElseThrow(ListingNotFoundException::new);

        if (!listing.getSellerId().equals(event.sellerId())) {
            throw new ListingConflictException("seller not found");
        }

        if (listing.getStatus() == Status.PUBLISHED) {
            listing.reserve(event.buyerId());
            return;
        }

        // 이미 예약중이며 같은 예약자이면 패스
        if (listing.getStatus() == Status.RESERVED
                && event.buyerId().equals(listing.getReserverId())) {
            return;
        }

        throw new ListingConflictException("Cannot reserve listing for appointment");

    }

    @EventListener
    public void handle(TradeAppointmentCanceledEvent event) {
        listingRepository.findActiveById(event.listingId())
                .ifPresent(listing -> {
                    if (!event.buyerId().equals(listing.getReserverId())) {
                        return;
                    }

                    listing.cancelReserve();
                });
    }

}
