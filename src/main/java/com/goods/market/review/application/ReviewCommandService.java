package com.goods.market.review.application;

import com.goods.market.review.domain.Review;

public interface ReviewCommandService {

    Review create(Long tradeId, Long writerId, boolean isSeller, int rating, String comment);
}
