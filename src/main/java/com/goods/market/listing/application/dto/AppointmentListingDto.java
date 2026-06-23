package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.Listing;

public record AppointmentListingDto(
        Long listingId,
        String title,
        String status,
        Long reserverId
) {
    public static AppointmentListingDto from(Listing listing) {
        return new AppointmentListingDto(
                listing.getId(),
                listing.getTitle(),
                listing.getStatus().name(),
                listing.getReserverId()
        );
    }
}
