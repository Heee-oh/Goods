package com.goods.market.listing.presentation.dto.request;

import com.goods.market.listing.application.dto.ListingUpdateCommand;
import com.goods.market.listing.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ListingUpdateRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotNull
        String description,
        @NotNull
        Long categoryId,
        @NotNull
        Long priceAmount,
        @NotNull
        TransactionType transactionType,
        HopeLocationRequest hopeLocation,
        @NotNull
        List<@NotBlank String> imageUrls
) {
    public ListingUpdateCommand toCommand() {

        if (hopeLocation == null) {
            return new ListingUpdateCommand(
                    title,
                    description,
                    categoryId,
                    priceAmount,
                    transactionType,
                    null, null, null,
                    imageUrls
            );
        }

        return new ListingUpdateCommand(
                title,
                description,
                categoryId,
                priceAmount,
                transactionType,
                hopeLocation.regionId(),
                hopeLocation.lat(),
                hopeLocation.lng(),
                imageUrls
        );
    }

    public static record ListingsRequest(Integer regionId, Long lastListingId) {

    }
}
