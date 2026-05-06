package com.goods.market.listing.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ListingReserveRequest(
        @NotNull
        @Positive
        Long buyerId
) {
}

