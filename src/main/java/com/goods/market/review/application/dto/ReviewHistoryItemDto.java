package com.goods.market.review.application.dto;

import com.querydsl.core.annotations.QueryProjection;

public record ReviewHistoryItemDto(
        long reviewId,
        String comment,
        int rating,
        long listingId,
        String listingImageUrl
) {

    @QueryProjection
    public ReviewHistoryItemDto {
    }
}
