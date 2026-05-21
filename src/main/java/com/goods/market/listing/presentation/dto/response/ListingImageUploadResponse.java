package com.goods.market.listing.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ListingImageUploadResponse(
        @JsonProperty("imageUrl")
        String imageUrl
) {
}
