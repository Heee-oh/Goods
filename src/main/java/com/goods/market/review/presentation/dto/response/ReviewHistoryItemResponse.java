package com.goods.market.review.presentation.dto.response;

import com.goods.market.review.application.dto.ReviewHistoryItemDto;

public record ReviewHistoryItemResponse(
        long reviewId,
        String comment,
        int rating,
        long listingId,
        String listingImageUrl
) {

    public static ReviewHistoryItemResponse from(ReviewHistoryItemDto review) {
        return new ReviewHistoryItemResponse(
                review.reviewId(),
                review.comment(),
                review.rating(),
                review.listingId(),
                review.listingImageUrl()
        );
    }
}
