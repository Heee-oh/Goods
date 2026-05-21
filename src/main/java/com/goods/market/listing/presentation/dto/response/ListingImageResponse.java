package com.goods.market.listing.presentation.dto.response;

import com.goods.market.listing.application.dto.ListingImageDto;

public record ListingImageResponse(
        Long imageId,
        String imageUrl,
        int sortOrder
) {
    public static ListingImageResponse from(ListingImageDto dto) {
        return new ListingImageResponse(dto.imageId(), dto.imageUrl(), dto.sortOrder());
    }
}
