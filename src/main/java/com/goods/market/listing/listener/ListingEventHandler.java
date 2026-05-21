package com.goods.market.listing.listener;

import com.goods.market.common.event.events.TradeAppointmentCanceledEvent;
import com.goods.market.common.event.events.TradeAppointmentScheduledEvent;
import com.goods.market.listing.domain.Listing;
import com.goods.market.listing.domain.Status;
import com.goods.market.listing.exception.ListingConflictException;
import com.goods.market.listing.exception.ListingNotFoundException;
import com.goods.market.listing.infrastructure.ListingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingEventHandler {

    private final ListingJpaRepository listingJpaRepository;

    @EventListener
    public void handle(TradeAppointmentScheduledEvent event) {
        Listing listing = listingJpaRepository.findByIdAndDeletedAtIsNull(event.listingId())
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
        listingJpaRepository.findByIdAndDeletedAtIsNull(event.listingId())
                .ifPresent(listing -> {
                    if (!event.buyerId().equals(listing.getReserverId())) {
                        return;
                    }

                    listing.cancelReserve();
                });
    }

}
