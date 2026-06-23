package com.goods.market.review.presentation.dto.response;

import com.goods.market.review.domain.Review;

public record ReviewResponse(
        Long reviewId,
        Long tradeId,
        Long listingId,
        Long writerId,
        Long targetId,
        int rating,
        String comment
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getTradeId(),
                review.getListingId(),
                review.getWriterId(),
                review.getTargetId(),
                review.getRating(),
                review.getComment()
        );
    }
}
