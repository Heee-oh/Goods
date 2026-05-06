package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.ListingImage;

public record ListingImageResponse(
        Long imageId,
        String imageUrl,
        int sortOrder
) {
    public static ListingImageResponse from(ListingImage image) {
        return new ListingImageResponse(
                image.getImageId(),
                image.getImageUrl(),
                image.getSortOrder()
        );
    }
}

