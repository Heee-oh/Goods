package com.goods.market.listing.application.dto;

import com.goods.market.listing.domain.ListingImage;

public record ListingImageDto(
        Long imageId,
        String imageUrl,
        int sortOrder
) {
    public static ListingImageDto from(ListingImage image) {
        return new ListingImageDto(
                image.getImageId(),
                image.getImageUrl(),
                image.getSortOrder()
        );
    }
}
