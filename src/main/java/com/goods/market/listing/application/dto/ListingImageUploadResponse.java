package com.goods.market.listing.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ListingImageUploadResponse(
        @JsonProperty("imageUrl")
        String imageUrl
) {
}
